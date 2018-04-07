package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class AutoPollingPolicyTest {
    @Test
    public void getCacheFails() throws Exception {
        String result = "test";

        ConfigFetcher fetcher = mock(ConfigFetcher.class);
        ConfigCache cache = mock(ConfigCache.class);

        doThrow(new Exception()).when(cache).read();
        doThrow(new Exception()).when(cache).write(anyString());

        when(cache.get()).thenCallRealMethod();
        doCallRealMethod().when(cache).set(anyString());

        when(fetcher.getConfigurationJsonStringAsync())
                .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.FETCHED, result)));

        AutoPollingPolicy policy = AutoPollingPolicy.newBuilder()
                .autoPollIntervalInSeconds(2)
                .build(fetcher,cache);

        assertEquals(result, policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getFetchedSameResponseNotUpdatesCache() throws Exception {
        String result = "test";

        ConfigFetcher fetcher = mock(ConfigFetcher.class);
        ConfigCache cache = mock(ConfigCache.class);

        when(cache.get()).thenReturn(result);

        when(fetcher.getConfigurationJsonStringAsync())
                .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.FETCHED, result)));

        AutoPollingPolicy policy = AutoPollingPolicy.newBuilder()
                .autoPollIntervalInSeconds(2)
                .build(fetcher,cache);

        assertEquals("test", policy.getConfigurationJsonAsync().get());

        verify(cache, never()).write(result);
    }

    @Test
    public void configChanged() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "");
        ConfigCache cache = new InMemoryConfigCache();
        fetcher.setUrl(server.url("/").toString());

        AtomicReference<String> newConfig  = new AtomicReference<>();

        AutoPollingPolicy policy = AutoPollingPolicy.newBuilder()
                .autoPollIntervalInSeconds(2)
                .configurationChangeListener((parser, newConfiguration) -> newConfig.set(newConfiguration))
                .build(fetcher, cache);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("test2"));

        Thread.sleep(1000);

        assertEquals("test", newConfig.get());

        Thread.sleep(2000);

        assertEquals("test2", newConfig.get());

        server.close();
        policy.close();
    }

    @Test
    public void throwsWhenListenerNull() {
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "");
        ConfigCache cache = new InMemoryConfigCache();
        assertThrows(IllegalArgumentException.class, ()-> AutoPollingPolicy.newBuilder()
                .configurationChangeListener(null)
                .build(fetcher, cache));
    }
}
