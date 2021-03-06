package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ManualPollingPolicyTest {
    private RefreshPolicy policy;
    private MockWebServer server;
    private final Logger logger = LoggerFactory.getLogger(ManualPollingPolicyTest.class);

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode mode = PollingModes.ManualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigCache cache = new InMemoryConfigCache();
        this.policy = new ManualPollingPolicy(fetcher, cache, logger,"");
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.policy.close();
        this.server.shutdown();
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2").setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        this.policy.refreshAsync().get();
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //next call will get the new value
        this.policy.refreshAsync().get();
        assertEquals("test2", this.policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes.ManualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger,"", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        RefreshPolicy lPolicy = new ManualPollingPolicy(fetcher, new FailingCache(), logger,"");

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2").setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        lPolicy.refreshAsync().get();
        assertEquals("test", lPolicy.getConfigurationJsonAsync().get());

        //next call will get the new value
        lPolicy.refreshAsync().get();
        assertEquals("test2", lPolicy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        this.policy.refreshAsync().get();
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //previous value returned because of the refresh failure
        this.policy.refreshAsync().get();
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getFetchedSameResponseUpdatesCache() throws Exception {
        String result = "test";

        ConfigFetcher fetcher = mock(ConfigFetcher.class);
        ConfigCache cache = mock(ConfigCache.class);

        when(cache.read(anyString())).thenReturn(result);

        when(fetcher.getConfigurationJsonStringAsync())
                .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.FETCHED, result)));

        ManualPollingPolicy policy = new ManualPollingPolicy(fetcher, cache, logger,"");
        policy.refreshAsync().get();
        assertEquals("test", policy.getConfigurationJsonAsync().get());

        verify(cache, atMostOnce()).write(anyString(), eq(result));
    }
}