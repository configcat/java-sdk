package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class AutoPollingPolicyTest {
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(AutoPollingPolicyTest.class), LogLevel.WARNING);
    private final ConfigMemoryCache memoryCache = new ConfigMemoryCache(logger);

    @Test
    public void getCacheFails() throws Exception {
        String result = "test";

        ConfigFetcher fetcher = mock(ConfigFetcher.class);
        ConfigCache cache = mock(ConfigCache.class);

        doThrow(new Exception()).when(cache).read(anyString());
        doThrow(new Exception()).when(cache).write(anyString(), anyString());

        when(fetcher.getConfigurationAsync())
                .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.FETCHED, memoryCache.getConfigFromJson(String.format(TEST_JSON, result)))));

        DefaultRefreshPolicy policy = new AutoPollingPolicy(fetcher, cache, logger, new ConfigMemoryCache(logger), "", (AutoPollingMode) PollingModes.autoPoll(2));

        assertEquals(result, policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        policy.close();
    }

    @Test
    public void getFetchedSameResponseNotUpdatesCache() throws Exception {
        String result = "test";

        ConfigFetcher fetcher = mock(ConfigFetcher.class);
        ConfigCache cache = mock(ConfigCache.class);

        when(cache.read(anyString())).thenReturn(String.format(TEST_JSON, result));

        when(fetcher.getConfigurationAsync())
                .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.FETCHED, memoryCache.getConfigFromJson(String.format(TEST_JSON, result)))));

        DefaultRefreshPolicy policy = new AutoPollingPolicy(fetcher, cache, logger, new ConfigMemoryCache(logger), "", (AutoPollingMode) PollingModes.autoPoll(2));
        assertEquals(result, policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());

        verify(cache, never()).write(anyString(), eq(result));

        policy.close();
    }

    @Test
    public void configChanged() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.start();

        AtomicBoolean isCalled = new AtomicBoolean();
        PollingMode mode = PollingModes
                .autoPoll(2, () -> isCalled.set(true));
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, new ConfigMemoryCache(logger),
                "", server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigCache cache = new InMemoryConfigCache();

        DefaultRefreshPolicy policy = new AutoPollingPolicy(fetcher, cache, logger, new ConfigMemoryCache(logger), "", (AutoPollingMode) mode);

        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        Thread.sleep(1000);

        assertTrue(isCalled.get());

        server.close();
        policy.close();
    }
}
