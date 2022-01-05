package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ManualPollingPolicyTest {
    private RefreshPolicyBase policy;
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(ManualPollingPolicyTest.class));
    private final ConfigMemoryCache memoryCache = new ConfigMemoryCache(logger);
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, new ConfigMemoryCache(logger), "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigCache cache = new InMemoryConfigCache();
        this.policy = new ManualPollingPolicy(fetcher, cache, logger, new ConfigMemoryCache(logger), "");
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.policy.close();
        this.server.shutdown();
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //first call
        this.policy.refreshAsync().get();
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        //next call will get the new value
        this.policy.refreshAsync().get();
        assertEquals("test2", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, new ConfigMemoryCache(logger), "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        RefreshPolicyBase lPolicy = new ManualPollingPolicy(fetcher, new FailingCache(), logger, new ConfigMemoryCache(logger), "");

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //first call
        lPolicy.refreshAsync().get();
        assertEquals("test", lPolicy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        //next call will get the new value
        lPolicy.refreshAsync().get();
        assertEquals("test2", lPolicy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        this.policy.refreshAsync().get();
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        //previous value returned because of the refresh failure
        this.policy.refreshAsync().get();
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
    }

    @Test
    public void getFetchedSameResponseUpdatesCache() throws Exception {
        String result = "test";

        ConfigFetcher fetcher = mock(ConfigFetcher.class);
        ConfigCache cache = mock(ConfigCache.class);

        when(cache.read(anyString())).thenReturn(String.format(TEST_JSON, result));

        when(fetcher.fetchAsync())
                .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.FETCHED, memoryCache.getConfigFromJson(String.format(TEST_JSON, result)))));

        ManualPollingPolicy policy = new ManualPollingPolicy(fetcher, cache, logger, new ConfigMemoryCache(logger), "");
        policy.refreshAsync().get();
        assertEquals(result, policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        verify(cache, atMostOnce()).write(anyString(), eq(String.format(TEST_JSON, result)));
    }
}