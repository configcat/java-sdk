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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class AutoPollingPolicyTest {
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(AutoPollingPolicyTest.class), LogLevel.WARNING);
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";

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
    public void ensuresPollingIntervalGreaterThanTwoSeconds() {
        assertThrows(IllegalArgumentException.class, () -> PollingModes.autoPoll(1));
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(3, TimeUnit.SECONDS));

        ConfigCache cache = new NullConfigCache();
        ConfigJsonCache memoryCache = new ConfigJsonCache(logger, cache, "");
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                memoryCache,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, (AutoPollingMode) pollingMode, cache, logger, false);


        //first call
        assertEquals("test", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());

        //wait for cache refresh
        Thread.sleep(6000);

        //next call will get the new value
        assertEquals("test2", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());
    }

    @Test
    public void getFail() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        ConfigCache cache = new NullConfigCache();
        ConfigJsonCache memoryCache = new ConfigJsonCache(logger, cache, "");
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                memoryCache,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, (AutoPollingMode) pollingMode, cache, logger, false);

        //first call
        assertTrue(configService.getSettingsAsync().get().isEmpty());
    }

    @Test
    public void getMany() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test3")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test4")));

        ConfigCache cache = new NullConfigCache();
        ConfigJsonCache memoryCache = new ConfigJsonCache(logger, cache, "");
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                memoryCache,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, (AutoPollingMode) pollingMode, cache, logger, false);


        //first calls
        assertEquals("test", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());
        assertEquals("test", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());
        assertEquals("test", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test2", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test3", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test4", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        ConfigCache cache = new NullConfigCache();
        ConfigJsonCache memoryCache = new ConfigJsonCache(logger, cache, "");
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                memoryCache,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, (AutoPollingMode) pollingMode, cache, logger, false);

        //first call
        assertEquals("test", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());

        //wait for cache invalidation
        Thread.sleep(3000);

        //previous value returned because of the refresh failure
        assertEquals("test", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());
    }

    @Test
    public void configChanged() throws IOException, InterruptedException {
        AtomicBoolean isCalled = new AtomicBoolean();
        ConfigCache cache = new NullConfigCache();
        ConfigJsonCache memoryCache = new ConfigJsonCache(logger, cache, "");
        PollingMode pollingMode = PollingModes
                .autoPoll(2, () -> isCalled.set(true));
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, memoryCache,
                "", server.url("/").toString(), false, pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, (AutoPollingMode) pollingMode, cache, logger, false);

        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        Thread.sleep(1000);

        assertTrue(isCalled.get());

        configService.close();
    }

    @Test
    public void getCacheFails() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        ConfigCache cache = mock(ConfigCache.class);

        doThrow(new Exception()).when(cache).read(anyString());
        doThrow(new Exception()).when(cache).write(anyString(), anyString());

        ConfigJsonCache memoryCache = new ConfigJsonCache(logger, cache, "");
        PollingMode pollingMode = PollingModes.autoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                memoryCache,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, (AutoPollingMode) pollingMode, cache, logger, false);

        assertEquals("test", configService.getSettingsAsync().get().get("fakeKey").value.getAsString());

        configService.close();
    }
}

