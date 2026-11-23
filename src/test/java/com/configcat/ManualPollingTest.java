package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class ManualPollingTest {
    private ConfigService configService;
    private MockWebServer server;
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(ManualPollingTest.class));
    private static final String TEST_JSON = "{ p: { s: 'test-salt'}, f: { fakeKey: { v: { s: %s }, p: [], r: [] } } }";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                mode.getPollingIdentifier());
        this.configService = new ConfigService("", fetcher, mode, new NullConfigCache(), logger, false, new ConfigCatHooks());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.configService.close();
        this.server.shutdown();
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //first call
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //next call will get the new value
        this.configService.refresh().get();
        assertEquals("test2", this.configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                mode.getPollingIdentifier());
        ConfigService configService = new ConfigService("", fetcher, mode, new FailingCache(), logger, false, new ConfigCatHooks());

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")).setBodyDelay(2, TimeUnit.SECONDS));

        //first call
        configService.refresh().get();
        assertEquals("test", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //next call will get the new value
        configService.refresh().get();
        assertEquals("test2", configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(500));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        //previous value returned because of the refresh failure
        this.configService.refresh().get();
        assertEquals("test", this.configService.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());
    }

    @Test
    void testCache() throws InterruptedException, ExecutionException, IOException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test2")));

        InMemoryCache cache = new InMemoryCache();
        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, mode, cache, logger, false, new ConfigCatHooks());

        service.refresh().get();
        assertEquals("test", service.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        service.refresh().get();
        assertEquals("test2", service.getSettings().get().settings().get("fakeKey").getSettingsValue().getStringValue());

        assertEquals(1, cache.getMap().size());

        service.close();
    }

    @Test
    void testNotModifiedDoesNotRefreshEntryReplacedDuringFetch() throws Exception {
        assertResponseDoesNotRefreshEntryReplacedDuringFetch(304, RefreshErrorCode.NONE);
    }

    @Test
    void testNonTransientErrorDoesNotRefreshEntryReplacedDuringFetch() throws Exception {
        assertResponseDoesNotRefreshEntryReplacedDuringFetch(404, RefreshErrorCode.INVALID_SDK_KEY);
    }

    private void assertResponseDoesNotRefreshEntryReplacedDuringFetch(int status, RefreshErrorCode expectedErrorCode) throws Exception {
        CompletableFuture<String> requestedETag = new CompletableFuture<>();
        CountDownLatch allowResponse = new CountDownLatch(1);
        this.server.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
                requestedETag.complete(request.getHeader("If-None-Match"));
                allowResponse.await(10, TimeUnit.SECONDS);
                return new MockResponse().setResponseCode(status);
            }
        });

        long now = System.currentTimeMillis();
        String original = Helpers.cacheValueFromConfigJsonWithEtagAndTime(String.format(TEST_JSON, "original"), "original", now - 120000);
        String replacement = Helpers.cacheValueFromConfigJsonWithEtagAndTime(String.format(TEST_JSON, "replacement"), "replacement", now - 60000);
        SingleValueCache cache = new SingleValueCache(original);

        PollingMode mode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), logger, "", this.server.url("/").toString(), false, mode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, mode, cache, logger, false, new ConfigCatHooks());

        try {
            CompletableFuture<RefreshResult> refresh = service.refresh();
            long replacementFetchTime = Entry.fromString(replacement).getFetchTime();

            try {
                assertEquals("original", requestedETag.get(5, TimeUnit.SECONDS));
                cache.write("", replacement);
                SettingResult settings = service.getSettings().get();
                assertEquals("replacement", settings.settings().get("fakeKey").getSettingsValue().getStringValue());
                assertEquals(replacementFetchTime, settings.fetchTime());
            } finally {
                allowResponse.countDown();
            }

            RefreshResult result = refresh.get(5, TimeUnit.SECONDS);
            assertEquals(expectedErrorCode, result.errorCode());
            assertEquals(expectedErrorCode == RefreshErrorCode.NONE, result.isSuccess());
            assertEquals(replacementFetchTime, service.getSettings().get().fetchTime());
            assertEquals(replacement, cache.read(""));
        } finally {
            service.close();
        }
    }

    @Test
    void testEmptyCacheDoesNotInitiateHTTP() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        assertTrue(this.configService.getSettings().get().settings().isEmpty());
        assertEquals(0, this.server.getRequestCount());
    }

    @Test
    void testOnlineOffline() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, false, new ConfigCatHooks());

        assertFalse(service.isOffline());
        assertTrue(service.refresh().get().isSuccess());
        assertEquals(1, this.server.getRequestCount());

        service.setOffline();

        assertTrue(service.isOffline());
        assertFalse(service.refresh().get().isSuccess());
        assertEquals(1, this.server.getRequestCount());

        service.setOnline();

        assertFalse(service.isOffline());
        assertTrue(service.refresh().get().isSuccess());
        assertEquals(2, this.server.getRequestCount());

        service.close();
    }

    @Test
    void testInitOffline() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, "test")));

        PollingMode pollingMode = PollingModes.manualPoll();
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient(),
                logger,
                "",
                this.server.url("/").toString(),
                false,
                pollingMode.getPollingIdentifier());
        ConfigService service = new ConfigService("", fetcher, pollingMode, new NullConfigCache(), logger, true, new ConfigCatHooks());

        assertTrue(service.isOffline());
        assertFalse(service.refresh().get().isSuccess());
        assertEquals(0, this.server.getRequestCount());

        service.setOnline();
        assertFalse(service.isOffline());

        assertTrue(service.refresh().get().isSuccess());
        assertEquals(1, this.server.getRequestCount());

        service.close();
    }
}