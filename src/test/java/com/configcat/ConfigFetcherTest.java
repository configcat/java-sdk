package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;


public class ConfigFetcherTest {
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(ConfigFetcherTest.class), LogLevel.WARNING);
    private static final String TEST_JSON = "{ f: { fakeKey: { v: fakeValue, s: 0, p: [] ,r: [] } } }";
    private static final String TEST_JSON2 = "{ f: { fakeKey: { v: fakeValue2, s: 0, p: [] ,r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.server.shutdown();
    }

    @Test
    public void getConfigurationJsonStringETag() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON).setHeader("ETag", "fakeETag"));
        this.server.enqueue(new MockResponse().setResponseCode(304));

        ConfigJsonCache cache = new ConfigJsonCache(logger, new NullConfigCache(), "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, cache,
                "", this.server.url("/").toString(), false, PollingModes.manualPoll().getPollingIdentifier());

        FetchResponse fResult = fetcher.fetchAsync().get();
        cache.writeToCache(fResult.config());

        assertEquals("fakeValue", fResult.config().entries.get("fakeKey").value.getAsString());
        assertTrue(fResult.isFetched());
        assertFalse(fResult.isNotModified());
        assertFalse(fResult.isFailed());

        FetchResponse notModifiedResponse = fetcher.fetchAsync().get();
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
                new ConfigJsonCache(logger, new NullConfigCache(), ""),
                "",
                this.server.url("/").toString(),
                false,
                PollingModes.manualPoll().getPollingIdentifier());

        this.server.enqueue(new MockResponse().setBody("test").setBodyDelay(2, TimeUnit.SECONDS));

        assertTrue(fetch.fetchAsync().get().isFailed());
        assertEquals(Config.empty, fetch.fetchAsync().get().config());

        fetch.close();
    }

    @Test
    public void getFetchedETagNotUpdatesCache() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON).setHeader("ETag", "fakeETag"));
        this.server.enqueue(new MockResponse().setResponseCode(304));

        ConfigCache cache = mock(ConfigCache.class);
        when(cache.read(anyString())).thenReturn(TEST_JSON);
        ConfigJsonCache configJsonCache = new ConfigJsonCache(logger, cache, "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, configJsonCache,
                "", this.server.url("/").toString(), false, PollingModes.manualPoll().getPollingIdentifier());

        RefreshPolicyBase policy = new AutoPollingPolicy(fetcher, logger, configJsonCache, (AutoPollingMode) PollingModes.autoPoll(2));
        assertEquals("fakeValue", policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        verify(cache, never()).write(anyString(), eq(TEST_JSON));

        policy.close();
    }

    @Test
    public void getFetchedSameResponseNotUpdatesCache() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));

        ConfigCache cache = mock(ConfigCache.class);
        when(cache.read(anyString())).thenReturn(TEST_JSON);
        ConfigJsonCache configJsonCache = new ConfigJsonCache(logger, cache, "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, configJsonCache,
                "", this.server.url("/").toString(), false, PollingModes.manualPoll().getPollingIdentifier());

        RefreshPolicyBase policy = new AutoPollingPolicy(fetcher, logger, configJsonCache, (AutoPollingMode) PollingModes.autoPoll(2));
        assertEquals("fakeValue", policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        verify(cache, never()).write(anyString(), eq(TEST_JSON));

        policy.close();
    }

    @Test
    public void getCacheFails() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        ConfigCache cache = mock(ConfigCache.class);

        doThrow(new Exception()).when(cache).read(anyString());
        doThrow(new Exception()).when(cache).write(anyString(), anyString());

        ConfigJsonCache configJsonCache = new ConfigJsonCache(logger, cache, "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, configJsonCache,
                "", this.server.url("/").toString(), false, PollingModes.manualPoll().getPollingIdentifier());

        assertEquals("fakeValue", fetcher.fetchAsync().get().config().entries.get("fakeKey").value.getAsString());

        fetcher.close();
    }

    @Test
    public void cacheWriteFails() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON2));
        ConfigCache cache = mock(ConfigCache.class);

        Gson gson = new GsonBuilder().create();
        Config config = gson.fromJson(TEST_JSON, Config.class);
        config.timeStamp = Instant.now().getEpochSecond();

        when(cache.read(anyString())).thenReturn(gson.toJson(config));
        doThrow(new Exception()).when(cache).write(anyString(), anyString());

        ConfigJsonCache configJsonCache = new ConfigJsonCache(logger, cache, "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, configJsonCache,
                "", this.server.url("/").toString(), false, PollingModes.manualPoll().getPollingIdentifier());

        FetchResponse result = fetcher.fetchAsync().get();
        configJsonCache.writeToCache(result.config());

        assertEquals("fakeValue2", configJsonCache.readFromCache().entries.get("fakeKey").value.getAsString());

        fetcher.close();
    }

    @Test
    public void cacheWriteFailsCachedTakesPrecedence() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON2));
        ConfigCache cache = mock(ConfigCache.class);

        Gson gson = new GsonBuilder().create();
        Config config = gson.fromJson(TEST_JSON, Config.class);
        config.timeStamp = Instant.now().getEpochSecond() + 50;

        when(cache.read(anyString())).thenReturn(gson.toJson(config));
        doThrow(new Exception()).when(cache).write(anyString(), anyString());

        ConfigJsonCache configJsonCache = new ConfigJsonCache(logger, cache, "");
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, configJsonCache,
                "", this.server.url("/").toString(), false, PollingModes.manualPoll().getPollingIdentifier());

        FetchResponse result = fetcher.fetchAsync().get();
        configJsonCache.writeToCache(result.config());

        assertEquals("fakeValue", configJsonCache.readFromCache().entries.get("fakeKey").value.getAsString());

        fetcher.close();
    }

    @Test
    public void testIntegration() throws IOException, ExecutionException, InterruptedException {
        ConfigJsonCache cache = new ConfigJsonCache(logger, new NullConfigCache(), "PKDVCLf-Hq-h-kCzMp-L7Q/PaDVCFk9EpmD6sLpGLltTA");
        ConfigFetcher fetch = new ConfigFetcher(new OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.SECONDS)
                .build(),
                logger,
                cache,
                "PKDVCLf-Hq-h-kCzMp-L7Q/PaDVCFk9EpmD6sLpGLltTA",
                "https://cdn-global.configcat.com",
                false,
                PollingModes.manualPoll().getPollingIdentifier());

        FetchResponse result = fetch.fetchAsync().get();
        cache.writeToCache(result.config());

        assertTrue(result.isFetched());
        assertTrue(fetch.fetchAsync().get().isNotModified());

        fetch.close();
    }
}
