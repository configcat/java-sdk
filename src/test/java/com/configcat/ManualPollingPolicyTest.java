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

import static org.junit.Assert.assertEquals;

public class ManualPollingPolicyTest {
    private ConfigService configService;
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(ManualPollingPolicyTest.class));
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode mode = PollingModes.manualPoll();
        ConfigJsonCache memoryCache = new ConfigJsonCache(logger, new NullConfigCache(), "");
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
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //first call
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").value.getAsString());

        //next call will get the new value
        this.configService.refresh().get();
        assertEquals("test2", this.configService.getSettings().get().settings().get("fakeKey").value.getAsString());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes.manualPoll();
        ConfigJsonCache cache = new ConfigJsonCache(logger, new FailingCache(), "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, cache, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, mode, new FailingCache(), logger, false);

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //first call
        configService.refresh().get();
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").value.getAsString());

        //next call will get the new value
        configService.refresh().get();
        assertEquals("test2", configService.getSettings().get().settings().get("fakeKey").value.getAsString());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").value.getAsString());

        //previous value returned because of the refresh failure
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").value.getAsString());
    }

    //TODO replace with offline tests
    /** @Test public void getFetchedSameResponseUpdatesCache() throws Exception {
    String result = "test";
    ConfigCache cache = mock(ConfigCache.class);
    ConfigJsonCache memoryCache = new ConfigJsonCache(logger, cache, "");
    ConfigFetcher fetcher = mock(ConfigFetcher.class);
    when(cache.read(anyString())).thenReturn(String.format(TEST_JSON, result));
    when(fetcher.fetchAsync())
    .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.FETCHED, new Entry(memoryCache.readConfigFromJson(String.format(TEST_JSON, result)).value(), "", System.currentTimeMillis()), "")));
    ConfigService configService = new ConfigService("", fetcher, PollingModes.manualPoll(), new FailingCache(), logger, false);
    configService.refresh().get();
    assertEquals(result, configService.getSettings().get().settings().get("fakeKey").value.getAsString());
    verify(cache, atMostOnce()).write(anyString(), eq(String.format(TEST_JSON, result)));
    }
     */
}