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


public class ConfigFetcherTest {
    private MockWebServer server;
    private ConfigFetcher fetcher;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(ConfigFetcherTest.class), LogLevel.WARNING);

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, new ConfigMemoryCache(logger),
                "", this.server.url("/").toString(), false, PollingModes.ManualPoll().getPollingIdentifier());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fetcher.close();
        this.server.shutdown();
    }

    @Test
    public void getConfigurationJsonStringETag() throws InterruptedException, ExecutionException {
        String result = "{ f: { fakeKey: { v: fakeValue, s: 0, p: [] ,r: [] } } }";
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(result).setHeader("ETag", "fakeETag"));
        this.server.enqueue(new MockResponse().setResponseCode(304));

        FetchResponse fResult = this.fetcher.getConfigurationAsync().get();

        assertEquals(result, fResult.config().JsonString);
        assertTrue(fResult.isFetched());
        assertFalse(fResult.isNotModified());
        assertFalse(fResult.isFailed());

        FetchResponse notModifiedResponse = this.fetcher.getConfigurationAsync().get();
        assertTrue(notModifiedResponse.isNotModified());
        assertFalse(notModifiedResponse.isFailed());
        assertFalse(notModifiedResponse.isFetched());

        assertNull(this.server.takeRequest().getHeader("If-None-Match"));
        assertEquals("fakeETag", this.server.takeRequest().getHeader("If-None-Match"));
    }

    @Test
    public void getConfigurationException() throws IOException, ExecutionException, InterruptedException {

        ConfigFetcher fetch = new ConfigFetcher(new OkHttpClient.Builder()
                    .readTimeout(1, TimeUnit.SECONDS)
                    .build(),
                logger,
                new ConfigMemoryCache(logger),
                "",
                this.server.url("/").toString(),
                false,
                PollingModes.ManualPoll().getPollingIdentifier());

        this.server.enqueue(new MockResponse().setBody("test").setBodyDelay(2, TimeUnit.SECONDS));

        assertTrue(fetch.getConfigurationAsync().get().isFailed());
        assertNull(fetch.getConfigurationAsync().get().config());

        fetch.close();
    }

    @Test
    public void testIntegration() throws IOException, ExecutionException, InterruptedException {

        ConfigFetcher fetch = new ConfigFetcher(new OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.SECONDS)
                .build(),
                logger,
                new ConfigMemoryCache(logger),
                "PKDVCLf-Hq-h-kCzMp-L7Q/PaDVCFk9EpmD6sLpGLltTA",
                "https://cdn-global.configcat.com",
                false,
                PollingModes.ManualPoll().getPollingIdentifier());

        assertTrue(fetch.getConfigurationAsync().get().isFetched());
        assertTrue(fetch.getConfigurationAsync().get().isNotModified());

        fetch.close();
    }
}
