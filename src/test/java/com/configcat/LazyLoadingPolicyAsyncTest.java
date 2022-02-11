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

public class LazyLoadingPolicyAsyncTest {
    private RefreshPolicyBase policy;
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(LazyLoadingPolicyAsyncTest.class));
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode mode = PollingModes
                .lazyLoad(5, true);
        ConfigJsonCache cache = new ConfigJsonCache(logger, new NullConfigCache(), "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, cache, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        this.policy = new LazyLoadingPolicy(fetcher, logger, cache, (LazyLoadingMode) mode);
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

        //simulate quick first calls
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        //wait for cache invalidation
        Thread.sleep(6000);

        //previous value returned until the new is not fetched
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        //wait for refresh response
        Thread.sleep(3000);

        //new value is present
        assertEquals("test2", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes
                .lazyLoad(5, true);
        ConfigJsonCache cache = new ConfigJsonCache(logger, new FailingCache(), "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, cache, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        RefreshPolicyBase lPolicy = new LazyLoadingPolicy(fetcher, logger, cache, (LazyLoadingMode) mode);

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //simulate quick first calls
        assertEquals("test", lPolicy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
        assertEquals("test", lPolicy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        ///wait for cache invalidation
        Thread.sleep(6000);

        //previous value returned until the new is not fetched
        assertEquals("test", lPolicy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        //wait for refresh response
        Thread.sleep(3000);

        //new value is present
        assertEquals("test2", lPolicy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //simulate quick first calls
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        //wait for cache invalidation
        Thread.sleep(6000);

        //trigger reload
        this.policy.getConfigurationAsync().get();

        //wait for refresh response
        Thread.sleep(1000);

        //previous value returned because of the refresh failure
        assertEquals("test", this.policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
    }
}