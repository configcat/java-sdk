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


public class ConfigFetcherTest {
    private MockWebServer server;
    private ConfigFetcher fetcher;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "");
        this.fetcher.setUrl(this.server.url("/").toString());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fetcher.close();
        this.server.shutdown();
    }

    @Test
    public void getConfigurationJsonStringETag() throws InterruptedException, ExecutionException {
        String result = "test";
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(result).setHeader("ETag", "fakeETag"));
        this.server.enqueue(new MockResponse().setResponseCode(304));

        FetchResponse fResult = this.fetcher.getConfigurationJsonStringAsync().get();

        assertEquals(result, fResult.config());
        assertTrue(fResult.isFetched());
        assertFalse(fResult.isNotModified());
        assertFalse(fResult.isFailed());

        FetchResponse notModifiedResponse = this.fetcher.getConfigurationJsonStringAsync().get();
        assertTrue(notModifiedResponse.isNotModified());
        assertFalse(notModifiedResponse.isFailed());
        assertFalse(notModifiedResponse.isFetched());

        assertNull(this.server.takeRequest().getHeader("If-None-Match"));
        assertEquals("fakeETag", this.server.takeRequest().getHeader("If-None-Match"));
    }

    @Test
    public void getConfigurationException() throws IOException, ExecutionException, InterruptedException {

        ConfigFetcher fetch = new ConfigFetcher(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build(), "");
        fetch.setUrl(this.server.url("/").toString());

        this.server.enqueue(new MockResponse().setBody("test").setBodyDelay(5, TimeUnit.SECONDS));

        assertTrue(fetch.getConfigurationJsonStringAsync().get().isFailed());
        assertEquals(null, fetch.getConfigurationJsonStringAsync().get().config());

        fetch.close();
    }
}
