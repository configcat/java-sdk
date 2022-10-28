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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LazyLoadingPolicySyncTest {
    private ConfigService configService;
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(LazyLoadingPolicySyncTest.class));
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        ConfigJsonCache memoryCache = new ConfigJsonCache(logger, new NullConfigCache(), "");
        LazyLoadingMode mode = (LazyLoadingMode) PollingModes
                .lazyLoad(5);

        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, memoryCache, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        this.configService = new ConfigService("", fetcher, mode, new NullConfigCache(), logger, false);
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.configService.close();
        this.server.shutdown();
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", this.configService.getSettingsAsync().get().get("fakeKey").value.getAsString());

        //wait for cache invalidation
        Thread.sleep(6000);

        //next call will block until the new value is fetched
        assertEquals("test2", this.configService.getSettingsAsync().get().get("fakeKey").value.getAsString());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes
                .lazyLoad(5);
        ConfigJsonCache cache = new ConfigJsonCache(logger, new FailingCache(), "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, cache, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigService configService1 = new ConfigService("", fetcher, mode, new FailingCache(), logger, false);

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", configService1.getSettingsAsync().get().get("fakeKey").value.getAsString());

        //wait for cache invalidation
        Thread.sleep(6000);

        //next call will block until the new value is fetched
        assertEquals("test2", configService1.getSettingsAsync().get().get("fakeKey").value.getAsString());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        assertEquals("test", this.configService.getSettingsAsync().get().get("fakeKey").value.getAsString());

        //wait for cache invalidation
        Thread.sleep(6000);

        //previous value returned because of the refresh failure
        assertEquals("test", this.configService.getSettingsAsync().get().get("fakeKey").value.getAsString());
    }
}