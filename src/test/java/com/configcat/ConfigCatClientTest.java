package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ConfigCatClientTest {

    private static final String APIKEY = "TEST_KEY";

    private static final String TEST_JSON = "{ f: { fakeKey: { v: fakeValue, s: 0, p: [] ,r: [] } } }";
    private static final String TEST_JSON_MULTIPLE = "{ f: { key1: { v: true, i: 'fakeId1', p: [] ,r: [] }, key2: { v: false, i: 'fakeId2', p: [] ,r: [] } } }";

    @Test
    public void ensuresApiKeyIsNotNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new ConfigCatClient(null));

        assertEquals("sdkKey is null or empty", exception.getMessage());

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.newBuilder().build(null));

        assertEquals("sdkKey is null or empty", builderException.getMessage());
    }

    @Test
    public void ensuresApiKeyIsNotEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new ConfigCatClient(""));

        assertEquals("sdkKey is null or empty", exception.getMessage());

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.newBuilder().build(""));

        assertEquals("sdkKey is null or empty", builderException.getMessage());
    }

    @Test
    public void getValueWithDefaultConfigTimeout() throws IOException {
        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .httpClient(new OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build())
                .build(APIKEY);

        // makes a call to a real url which would fail, default expected
        boolean config = cl.getValue(Boolean.class, "key", true);
        assertTrue(config);

        cl.close();
    }

    @Test
    public void getConfigurationWithFailingCache() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .cache(new FailingCache())
                .mode(PollingModes.ManualPoll())
                .baseUrl(server.url("/").toString())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        cl.forceRefresh();
        assertEquals("fakeValue", cl.getValue(String.class, "fakeKey", null));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationAutoPollFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .cache(new FailingCache())
                .mode(PollingModes.AutoPoll(5))
                .baseUrl(server.url("/").toString())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationExpCacheFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .cache(new FailingCache())
                .mode(PollingModes.LazyLoad(5))
                .baseUrl(server.url("/").toString())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationManualFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .cache(new FailingCache())
                .mode(PollingModes.ManualPoll())
                .baseUrl(server.url("/").toString())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationReturnsPreviousCachedOnTimeout() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .mode(PollingModes.ManualPoll())
                .baseUrl(server.url("/").toString())
                .httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("delayed").setBodyDelay(3, TimeUnit.SECONDS));

        cl.forceRefresh();
        assertEquals("fakeValue", cl.getValue(String.class, "fakeKey", null));
        cl.forceRefresh();
        assertEquals("fakeValue", cl.getValue(String.class, "fakeKey", null));

        server.close();
        cl.close();
    }

    @Test
    public void maxInitWaitTimeTest() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .mode(PollingModes.AutoPoll(60, 1))
                .baseUrl(server.url("/").toString())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("delayed").setBodyDelay(2, TimeUnit.SECONDS));

        Instant previous = Instant.now();
        assertNull(cl.getValue(String.class, "fakeKey", null));
        assertTrue(Duration.between(previous, Instant.now()).toMillis() < 1500);

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationReturnsPreviousCachedOnFailAsync() throws IOException, ExecutionException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .mode(PollingModes.ManualPoll())
                .baseUrl(server.url("/").toString())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        server.enqueue(new MockResponse().setResponseCode(500));

        cl.forceRefresh();
        assertEquals("fakeValue", cl.getValueAsync(String.class, "fakeKey", null).get());
        cl.forceRefresh();
        assertEquals("fakeValue", cl.getValueAsync(String.class, "fakeKey", null).get());

        server.close();
        cl.close();
    }

    @Test
    public void getValueReturnsDefaultOnExceptionRepeatedly() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .mode(PollingModes.ManualPoll())
                .baseUrl(server.url("/").toString())
                .httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build())
                .build(APIKEY);

        String badJson = "{ test: test] }";
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson).setBodyDelay(3, TimeUnit.SECONDS));

        cl.forceRefresh();
        assertSame(def, cl.getValue(String.class, "test", def));

        cl.forceRefresh();
        assertSame(def, cl.getValue(String.class, "test", def));

        server.shutdown();
        cl.close();
    }

    @Test
    public void forceRefreshWithTimeout() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .mode(PollingModes.ManualPoll())
                .baseUrl(server.url("/").toString())
                .httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("test").setBodyDelay(3, TimeUnit.SECONDS));

        cl.forceRefresh();

        server.shutdown();
        cl.close();
    }

    @Test
    public void getAllValues() throws IOException, ExecutionException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .mode(PollingModes.ManualPoll())
                .baseUrl(server.url("/").toString())
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_MULTIPLE));
        cl.forceRefresh();

        Map<String, Object> allValues = cl.getAllValues(null);

        assertEquals(true, allValues.get("key1"));
        assertEquals(false, allValues.get("key2"));

        server.shutdown();
        cl.close();
    }

    @Test
    public void getValueInvalidArguments() {
        ConfigCatClient client = new ConfigCatClient("key");
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class, null, false));
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class, "", false));

        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class, null, false).get());
        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class, "", false).get());
    }
}