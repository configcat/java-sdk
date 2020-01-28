package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class LazyLoadingPolicyAsyncTest {
    private RefreshPolicy policy;
    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode mode = PollingModes
                .LazyLoad(5, true);

        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "", this.server.url("/").toString(), mode);
        ConfigCache cache = new InMemoryConfigCache();
        this.policy = mode.accept(new RefreshPolicyFactory(cache, fetcher));
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.policy.close();
        this.server.shutdown();
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2").setBodyDelay(2, TimeUnit.SECONDS));

        //simulate quick first calls
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //wait for cache invalidation
        Thread.sleep(6000);

        //previous value returned until the new is not fetched
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //wait for refresh response
        Thread.sleep(3000);

        //new value is present
        assertEquals("test2", this.policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes
                .LazyLoad(5, true);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "", this.server.url("/").toString(), mode);
        RefreshPolicy lPolicy = mode.accept(new RefreshPolicyFactory(new FailingCache(), fetcher));

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2").setBodyDelay(2, TimeUnit.SECONDS));

        //simulate quick first calls
        assertEquals("test", lPolicy.getConfigurationJsonAsync().get());
        assertEquals("test", lPolicy.getConfigurationJsonAsync().get());

        ///wait for cache invalidation
        Thread.sleep(6000);

        //previous value returned until the new is not fetched
        assertEquals("test", lPolicy.getConfigurationJsonAsync().get());

        //wait for refresh response
        Thread.sleep(3000);

        //new value is present
        assertEquals("test2", lPolicy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //simulate quick first calls
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //wait for cache invalidation
        Thread.sleep(6000);

        //trigger reload
        this.policy.getConfigurationJsonAsync().get();

        //wait for refresh response
        Thread.sleep(1000);

        //previous value returned because of the refresh failure
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());
    }
}