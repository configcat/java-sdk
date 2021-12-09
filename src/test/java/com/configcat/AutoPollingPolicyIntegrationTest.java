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

public class AutoPollingPolicyIntegrationTest {
    private AutoPollingPolicy policy;
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(AutoPollingPolicyIntegrationTest.class), LogLevel.WARNING);
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode pollingMode = PollingModes.AutoPoll(2);
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                new ConfigMemoryCache(logger),
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigCache cache = new InMemoryConfigCache();
        this.policy = new AutoPollingPolicy(fetcher, cache, logger, new ConfigMemoryCache(logger), "", (AutoPollingMode)pollingMode);
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
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());

        //wait for cache refresh
        Thread.sleep(6000);

        //next call will get the new value
        assertEquals("test2", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());
    }

    @Test
    public void getFail() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        //first call
        assertNull(this.policy.getConfigurationAsync().get());
    }

    @Test
    public void getMany() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test3")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test4")));

        //first calls
        assertEquals("test", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());
        assertEquals("test", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());
        assertEquals("test", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test2", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test3", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());

        //wait for cache refresh
        Thread.sleep(2500);

        //next call will get the new value
        assertEquals("test4", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        assertEquals("test", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());

        //wait for cache invalidation
        Thread.sleep(3000);

        //previous value returned because of the refresh failure
        assertEquals("test", this.policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());
    }
}

