package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class ManualPollingPolicyTest {
    private ConfigService configService;
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(ManualPollingPolicyTest.class));
    private static final String TEST_JSON = "{ p: { s: 'test-slat'}, f: { fakeKey: { v: { s: %s }, p: [], r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                mode.getPollingIdentifier());
        this.configService = new ConfigService("", fetcher, mode, new NullConfigCache(), logger, false, new ConfigCatHooks());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.configService.close();
        this.server.shutdown();
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //first call
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //next call will get the new value
        this.configService.refresh().get();
        assertEquals("test2", this.configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                mode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, mode, new FailingCache(), logger, false, new ConfigCatHooks());

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //first call
        configService.refresh().get();
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //next call will get the new value
        configService.refresh().get();
        assertEquals("test2", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //previous value returned because of the refresh failure
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());
    }

    @Test
    void testCache() throws InterruptedException, ExecutionException, IOException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")));

        InMemoryCache cache = new InMemoryCache();
        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, mode, cache, logger, false, new ConfigCatHooks());

        service.refresh().get();
        assertEquals("test", service.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        service.refresh().get();
        assertEquals("test2", service.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        assertEquals(1, cache.getMap().size());

        service.close();
    }

    @Test
    void testEmptyCacheDoesNotInitiateHTTP() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        assertTrue(this.configService.getSettings().get().settings().isEmpty());
        assertEquals(0, this.server.getRequestCount());
    }

    @Test
    void testOnlineOffline() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, false, new ConfigCatHooks());

        assertFalse(service.isOffline());
        assertTrue(service.refresh().get().isSuccess());
        assertEquals(1, this.server.getRequestCount());

        service.setOffline();

        assertTrue(service.isOffline());
        assertFalse(service.refresh().get().isSuccess());
        assertEquals(1, this.server.getRequestCount());

        service.setOnline();

        assertFalse(service.isOffline());
        assertTrue(service.refresh().get().isSuccess());
        assertEquals(2, this.server.getRequestCount());

        service.close();
    }

    @Test
    void testInitOffline() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, true, new ConfigCatHooks());

        assertTrue(service.isOffline());
        assertFalse(service.refresh().get().isSuccess());
        assertEquals(0, this.server.getRequestCount());

        service.setOnline();
        assertFalse(service.isOffline());

        assertTrue(service.refresh().get().isSuccess());
        assertEquals(1, this.server.getRequestCount());

        service.close();
    }
}