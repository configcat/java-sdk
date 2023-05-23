package com.configcat;

import com.configcat.cache.ConfigCache;
import com.configcat.cache.FailingCache;
import com.configcat.cache.NullConfigCache;
import com.configcat.cache.SingleValueCache;
import com.configcat.polling.LazyLoadingMode;
import com.configcat.polling.PollingMode;
import com.configcat.polling.PollingModes;
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

public class LazyLoadingPolicyTest {
    private ConfigService configService;
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(LazyLoadingPolicyTest.class));
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        LazyLoadingMode mode = (LazyLoadingMode) PollingModes
                .lazyLoad(5);

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
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getValue().getAsString());

        //wait for cache invalidation
        Thread.sleep(6000);

        //next call will block until the new value is fetched
        assertEquals("test2", this.configService.getSettings().get().settings().get("fakeKey").getValue().getAsString());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes
                .lazyLoad(5);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger
                , "",
                this.server.url("/").toString(),
                false,
                mode.getPollingIdentifier());
        ConfigService configService1 = new ConfigService("", fetcher, mode, new FailingCache(), logger, false, new ConfigCatHooks());

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", configService1.getSettings().get().settings().get("fakeKey").getValue().getAsString());

        //wait for cache invalidation
        Thread.sleep(6000);

        //next call will block until the new value is fetched
        assertEquals("test2", configService1.getSettings().get().settings().get("fakeKey").getValue().getAsString());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getValue().getAsString());

        //wait for cache invalidation
        Thread.sleep(6000);

        //previous value returned because of the refresh failure
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getValue().getAsString());
    }

    @Test
    void testCacheExpirationRespectedInTTLCalc() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        ConfigCache cache = new SingleValueCache(Helpers.cacheValueFromConfigJson(String.format(TEST_JSON, "test")));

        PollingMode mode = PollingModes
                .lazyLoad(1);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, mode, cache, logger, false, new ConfigCatHooks());

        assertFalse(service.getSettings().get().settings().isEmpty());
        assertFalse(service.getSettings().get().settings().isEmpty());

        assertEquals(0, this.server.getRequestCount());

        Thread.sleep(1000);

        assertFalse(service.getSettings().get().settings().isEmpty());
        assertFalse(service.getSettings().get().settings().isEmpty());

        assertEquals(1, this.server.getRequestCount());
    }

    @Test
    void testCacheExpirationRespectedInTTLCalc304() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(304).setBody(""));

        ConfigCache cache = new SingleValueCache(Helpers.cacheValueFromConfigJson(String.format(TEST_JSON, "test")));

        PollingMode mode = PollingModes
                .lazyLoad(1);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, mode, cache, logger, false, new ConfigCatHooks());

        assertFalse(service.getSettings().get().settings().isEmpty());
        assertFalse(service.getSettings().get().settings().isEmpty());

        assertEquals(0, this.server.getRequestCount());

        Thread.sleep(1000);

        assertFalse(service.getSettings().get().settings().isEmpty());
        assertFalse(service.getSettings().get().settings().isEmpty());

        assertEquals(1, this.server.getRequestCount());
    }

    @Test
    void testOnlineOffline() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.lazyLoad(1);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, false, new ConfigCatHooks());

        assertFalse(service.getSettings().get().settings().isEmpty());
        assertEquals(1, this.server.getRequestCount());

        service.setOffline();
        assertTrue(service.isOffline());

        Thread.sleep(1500);

        assertFalse(service.getSettings().get().settings().isEmpty());
        assertEquals(1, this.server.getRequestCount());

        service.setOnline();
        assertFalse(service.isOffline());

        assertFalse(service.getSettings().get().settings().isEmpty());
        assertEquals(2, this.server.getRequestCount());

        service.close();
    }

    @Test
    void testInitOffline() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.lazyLoad(1);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, true, new ConfigCatHooks());

        assertTrue(service.getSettings().get().settings().isEmpty());
        assertEquals(0, this.server.getRequestCount());

        Thread.sleep(1500);

        assertTrue(service.getSettings().get().settings().isEmpty());
        assertEquals(0, this.server.getRequestCount());

        service.setOnline();
        assertFalse(service.isOffline());

        assertFalse(service.getSettings().get().settings().isEmpty());
        assertEquals(1, this.server.getRequestCount());

        service.close();
    }
}