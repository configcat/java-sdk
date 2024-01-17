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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class AutoPollingTest {
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(AutoPollingTest.class), LogLevel.WARNING);
    private static final String TEST_JSON = "{ p: { s: 'test-slat'}, f: { fakeKey: { v: {s: %s}, p: [] ,r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.server.shutdown();
    }

    @Test
    public void ensuresPollingIntervalGreaterThanOneSeconds() {
        assertThrows(IllegalArgumentException.class, () -> PollingModes.autoPoll(0));
    }

    @Test
    public void get() throws InterruptedException, ExecutionException, IOException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(3, TimeUnit.SECONDS));

        ConfigCache cache = new NullConfigCache();
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, cache, logger, false, new ConfigCatHooks());


        //first call
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //wait for cache refresh
        Thread.sleep(6000);

        //next call will get the new value
        assertEquals("test2", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        configService.close();
    }

    @Test
    public void getFail() throws InterruptedException, ExecutionException, IOException {
        this.server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        ConfigCache cache = new NullConfigCache();
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, cache, logger, false, new ConfigCatHooks());

        //first call
        assertTrue(configService.getSettings().get().settings().isEmpty());

        configService.close();
    }

    @Test
    public void getMany() throws InterruptedException, ExecutionException, IOException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test3")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test4")));

        ConfigCache cache = new NullConfigCache();
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, cache, logger, false, new ConfigCatHooks());

        //first calls
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test2", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test3", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test4", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        configService.close();
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException, IOException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        ConfigCache cache = new NullConfigCache();
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, cache, logger, false, new ConfigCatHooks());

        //first call
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //wait for cache invalidation
        Thread.sleep(3000);

        //previous value returned because of the refresh failure
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        configService.close();
    }

    @Test
    public void getCacheFails() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        ConfigCache cache = mock(ConfigCache.class);

        doThrow(new Exception()).when(cache).read(anyString());
        doThrow(new Exception()).when(cache).write(anyString(), anyString());

        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, cache, logger, false, new ConfigCatHooks());

        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        configService.close();
    }

    @Test
    void testInitWaitTimeTimeout() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")).setBodyDelay(2, TimeUnit.SECONDS));

        PollingMode pollingMode = PollingModes.autoPoll(60, 1);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, false, new ConfigCatHooks());

        long start = System.currentTimeMillis();
        assertTrue(configService.getSettings().get().settings().isEmpty());
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 1500);

        configService.close();
    }

    @Test
    void testPollIntervalRespectsCacheExpiration() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        ConfigCache cache = new SingleValueCache(Helpers.cacheValueFromConfigJson(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, cache, logger, false, new ConfigCatHooks());

        configService.getSettings().get();

        assertEquals(0, this.server.getRequestCount());

        Helpers.waitFor(3000, () -> this.server.getRequestCount() == 1);

        configService.close();
    }

    @Test
    void testOnlineOffline() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, false, new ConfigCatHooks());

        Thread.sleep(2500);

        configService.setOffline();
        assertTrue(configService.isOffline());
        assertEquals(2, this.server.getRequestCount());

        Thread.sleep(2000);

        assertEquals(2, this.server.getRequestCount());
        configService.setOnline();
        assertFalse(configService.isOffline());

        Helpers.waitFor(() -> this.server.getRequestCount() >= 3);

        configService.close();
    }

    @Test
    void testInitOffline() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, true, new ConfigCatHooks());

        assertTrue(configService.isOffline());
        assertEquals(0, this.server.getRequestCount());

        Thread.sleep(2000);

        assertEquals(0, this.server.getRequestCount());
        configService.setOnline();
        assertFalse(configService.isOffline());

        Helpers.waitFor(() -> this.server.getRequestCount() >= 2);

        configService.close();
    }

    @Test
    void testInitWaitTimeIgnoredWhenCacheIsNotExpired() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")).setBodyDelay(2, TimeUnit.SECONDS));

        ConfigCache cache = new SingleValueCache(Helpers.cacheValueFromConfigJson(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.autoPoll(60, 1);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, pollingMode, cache, logger, false, new ConfigCatHooks());

        long start = System.currentTimeMillis();
        assertFalse(configService.getSettings().get().settings().isEmpty());
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 1000);

        configService.close();
    }
}

