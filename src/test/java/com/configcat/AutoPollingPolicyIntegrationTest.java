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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class AutoPollingPolicyIntegrationTest {
    private AutoPollingPolicy policy;
    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode pollingMode = PollingModes.AutoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                "",
                this.server.url("/").toString(),
                PollingModes.AutoPoll(2));
        ConfigCache cache = new InMemoryConfigCache();
        this.policy = (AutoPollingPolicy)pollingMode.accept(new RefreshPolicyFactory(cache, fetcher));
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.policy.close();
        this.server.shutdown();
    }

    @Test
    public void ensuresPollingIntervalGreaterThanTwoSeconds() {
        assertThrows(IllegalArgumentException.class, ()-> PollingModes.AutoPoll(1));
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2").setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //wait for cache refresh
        Thread.sleep(6000);

        //next call will get the new value
        assertEquals("test2", this.policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getFail() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        //first call
        assertEquals(null, this.policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void configChanged() throws InterruptedException {
        AtomicReference<String> newConfig  = new AtomicReference<>();
        ConfigurationChangeListener listener = (parser, newConfiguration) -> newConfig.set(newConfiguration);
        this.policy.addConfigurationChangeListener(listener);

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2"));

        Thread.sleep(1000);

        assertEquals("test", newConfig.get());

        Thread.sleep(2000);

        assertEquals("test2", newConfig.get());

        this.policy.removeConfigurationChangeListener(listener);
    }

    @Test
    public void getMany() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test3"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test4"));

        //first calls
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test2", this.policy.getConfigurationJsonAsync().get());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test3", this.policy.getConfigurationJsonAsync().get());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test4", this.policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //wait for cache invalidation
        Thread.sleep(3000);

        //previous value returned because of the refresh failure
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());
    }
}

