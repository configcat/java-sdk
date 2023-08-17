package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigCatClientTest {
    private static final String TEST_JSON = "{ p: { s: 'test-slat' }, f: { fakeKey: {  t: 1, v: {s: 'fakeValue'}, s: 0, p: [] ,r: [] } } }";
    private static final String TEST_JSON_MULTIPLE = "{ p: { s: 'test-slat' }, f: { key1: { t: 0, v: {b: true}, i: 'fakeId1', p: [] ,r: [] }, key2: { t: 0, v: {b: false}, i: 'fakeId2', p: [] ,r: [] } } }";
    public static final String TEST_JSON_DEFAULT_USER = "{ p: { s: 'test-slat' }, 'f':{'fakeKey':{  t: 1, 'v': {s: 'defaultValue'},'i':'defaultId', r: [ {c: [ {t: { a: 'Identifier', c: 2, l: ['test1']}}],s: { v: {s: 'fakeValue1'},i: 'test1Id'}},{c: [{t: {a: 'Identifier', c: 2,l: ['test2']}}],s: { v: {s: 'fakeValue2'},i: 'test2Id'}}] } } }";
    public static final String RULES_JSON = "{ p: { s: 'test-slat' }, f: { key: {  t: 1, v: {s: 'def'}, t: 1, i: 'defVar', p: [] , r: [ {c: [ {t: { a: 'Identifier', c: 2, l: ['@test1.com']}}],s: { v: {s: 'fake1'},i: 'id1'}},{c: [{t: {a: 'Identifier', c: 2,l: ['@test2.com']}}],s: { v: {s: 'fake2'},i: 'id2'}}] } } }";

    @Test
    public void ensuresSDKKeyIsNotNull() {
        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.get(null));

        assertEquals("SDK Key cannot be null or empty.", builderException.getMessage());
    }

    @Test
    public void ensuresSDKKeyIsNotEmpty() {
        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.get(""));

        assertEquals("SDK Key cannot be null or empty.", builderException.getMessage());
    }

    @Test
    public void testSDKKeyValidation() throws IOException {

        //TEST VALID
        ConfigCatClient client = ConfigCatClient.get("sdk-key-90123456789012/1234567890123456789012");
        assertNotNull(client);
        client = ConfigCatClient.get("configcat-sdk-1/sdk-key-90123456789012/1234567890123456789012");
        assertNotNull(client);
        client = ConfigCatClient.get("configcat-proxy/sdk-key-90123456789012", options -> options.baseUrl("https://my-configcat-proxy"));
        assertNotNull(client);

        ConfigCatClient.closeAll();

        //TEST INVALID
        List<String> wrongSDKKeys = Arrays.asList(
                "sdk-key-90123456789012",
                "sdk-key-9012345678901/1234567890123456789012",
                "sdk-key-90123456789012/123456789012345678901",
                "sdk-key-90123456789012/12345678901234567890123",
                "sdk-key-901234567890123/1234567890123456789012",
                "configcat-sdk-1/sdk-key-90123456789012",
                "configcat-sdk-1/sdk-key-9012345678901/1234567890123456789012",
                "configcat-sdk-1/sdk-key-90123456789012/123456789012345678901",
                "configcat-sdk-1/sdk-key-90123456789012/12345678901234567890123",
                "configcat-sdk-1/sdk-key-901234567890123/1234567890123456789012",
                "configcat-sdk-2/sdk-key-90123456789012/1234567890123456789012",
                "configcat-proxy/",
                "configcat-proxy/sdk-key-90123456789012"
        );
        for (String sdkKey : wrongSDKKeys) {
            IllegalArgumentException builderException = assertThrows(
                    IllegalArgumentException.class, () -> ConfigCatClient.get(sdkKey));
            assertEquals("SDK Key '" + sdkKey + "' is invalid.", builderException.getMessage());
        }

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.get("configcat-proxy/", options -> options.baseUrl("https://my-configcat-proxy")));
        assertEquals("SDK Key 'configcat-proxy/' is invalid.", builderException.getMessage());

        ConfigCatClient.closeAll();
    }

    @Test
    public void getValueWithDefaultConfigTimeout() throws IOException {

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options ->
                options.httpClient(new OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()));

        // makes a call to a real url which would fail, default expected
        boolean config = cl.getValue(Boolean.class, "key", true);
        assertTrue(config);

        cl.close();
    }

    @Test
    public void getConfigurationWithFailingCache() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.cache(new FailingCache());
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

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

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.cache(new FailingCache());
            options.pollingMode(PollingModes.autoPoll(5));
            options.baseUrl(server.url("/").toString());
        });

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationExpCacheFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.cache(new FailingCache());
            options.pollingMode(PollingModes.lazyLoad(5));
            options.baseUrl(server.url("/").toString());
        });

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationManualFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.cache(new FailingCache());
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationReturnsPreviousCachedOnTimeout() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
            options.httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build());
        });

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

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.autoPoll(60, 1));
            options.baseUrl(server.url("/").toString());
        });

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

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

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

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
            options.httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build());
        });

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

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
            options.httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build());
        });

        server.enqueue(new MockResponse().setResponseCode(200).setBody("test").setBodyDelay(3, TimeUnit.SECONDS));

        cl.forceRefresh();

        server.shutdown();
        cl.close();
    }

    @Test
    public void getAllValues() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_MULTIPLE));
        cl.forceRefresh();

        Map<String, Object> allValues = cl.getAllValues(null);

        assertEquals(true, allValues.get("key1"));
        assertEquals(false, allValues.get("key2"));

        server.shutdown();
        cl.close();
    }

    @Test
    public void getAllValueDetails() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_MULTIPLE));
        cl.forceRefresh();

        List<EvaluationDetails<Object>> allValuesDetails = cl.getAllValueDetails(null);

        //assert result list
        assertEquals(2, allValuesDetails.size());

        //assert result 1
        EvaluationDetails<Object> element = allValuesDetails.get(0);
        assertEquals("key1", element.getKey());
        assertTrue((boolean) element.getValue());
        assertFalse(element.isDefaultValue());
        assertNull(element.getError());
        assertEquals("fakeId1", element.getVariationId());

        //assert result 2
        element = allValuesDetails.get(1);
        assertEquals("key2", element.getKey());
        assertFalse((boolean) element.getValue());
        assertFalse(element.isDefaultValue());
        assertNull(element.getError());
        assertEquals("fakeId2", element.getVariationId());
        server.shutdown();
        cl.close();
    }

    @Test
    public void getValueInvalidArguments() throws IOException {
        ConfigCatClient client = ConfigCatClient.get(Helpers.SDK_KEY);
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class, null, false));
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class, "", false));

        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class, null, false).get());
        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class, "", false).get());

        ConfigCatClient.closeAll();
    }

    @Test
    public void testDefaultUser() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        User user1 = User.newBuilder().build("test1");
        User user2 = User.newBuilder().build("test2");

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
            options.defaultUser(user2);
        });

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_DEFAULT_USER));
        cl.forceRefresh();

        //test build param
        assertEquals("fakeValue2", cl.getValue(String.class, "fakeKey", null, null));
        cl.clearDefaultUser();

        //without default user
        assertEquals("fakeValue1", cl.getValue(String.class, "fakeKey", user1, null));
        assertEquals("defaultValue", cl.getValue(String.class, "fakeKey", null, null));
        assertEquals("fakeValue2", cl.getValue(String.class, "fakeKey", user2, null));

        //manual set default user
        cl.setDefaultUser(user2);
        assertEquals("fakeValue1", cl.getValue(String.class, "fakeKey", user1, null));
        assertEquals("fakeValue2", cl.getValue(String.class, "fakeKey", null, null));

        //test clear
        cl.clearDefaultUser();
        assertEquals("defaultValue", cl.getValue(String.class, "fakeKey", null, null));

        server.shutdown();
        cl.close();
    }

    @Test
    void testDefaultUserVariationId() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(RULES_JSON));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        cl.forceRefresh();

        User user1 = new User.Builder().build("test@test1.com");
        User user2 = new User.Builder().build("test@test2.com");

        cl.setDefaultUser(user1);
        assertEquals("id1", cl.getValueDetails(String.class, "key", "").getVariationId());
        assertEquals("id2", cl.getValueDetails(String.class, "key", user2, "").getVariationId());

        cl.clearDefaultUser();

        assertEquals("defVar", cl.getValueDetails(String.class, "key", "").getVariationId());

        server.shutdown();
        cl.close();
    }

    @Test
    void testSingleton() throws IOException {
        ConfigCatClient client1 = ConfigCatClient.get(Helpers.SDK_KEY);
        ConfigCatClient client2 = ConfigCatClient.get(Helpers.SDK_KEY);

        assertSame(client1, client2);

        ConfigCatClient.closeAll();

        client1 = ConfigCatClient.get(Helpers.SDK_KEY);

        assertNotSame(client1, client2);

        ConfigCatClient.closeAll();
    }

    @Test
    void testSingletonOptions() throws IOException {
        ConfigCatClient client1 = ConfigCatClient.get(Helpers.SDK_KEY, client1Options -> client1Options.pollingMode(PollingModes.autoPoll()));

        ConfigCatClient client2 = ConfigCatClient.get(Helpers.SDK_KEY, client2Options -> client2Options.pollingMode(PollingModes.manualPoll()));

        assertSame(client1, client2);

        ConfigCatClient.closeAll();
    }

    @Test
    void testCloseRemovesTheClosingInstanceOnly() throws IOException {
        ConfigCatClient client1 = ConfigCatClient.get(Helpers.SDK_KEY);

        client1.close();

        ConfigCatClient client2 = ConfigCatClient.get(Helpers.SDK_KEY);

        assertNotSame(client1, client2);

        client1.close();

        ConfigCatClient client3 = ConfigCatClient.get(Helpers.SDK_KEY);

        assertSame(client2, client3);
    }

    @Test
    void testClose() throws IOException {
        ConfigCatClient client1 = ConfigCatClient.get(Helpers.SDK_KEY);
        assertFalse(client1.isClosed());
        client1.close();
        assertTrue(client1.isClosed());
    }

    @Test
    void testSingletonCloseAffects() throws IOException {

        ConfigCatClient client1 = ConfigCatClient.get(Helpers.SDK_KEY);
        client1.close();
        assertTrue(client1.isClosed());

        ConfigCatClient client2 = ConfigCatClient.get(Helpers.SDK_KEY);
        assertNotSame(client1, client2);
        client1.close();
        assertFalse(client2.isClosed());

        ConfigCatClient client3 = ConfigCatClient.get(Helpers.SDK_KEY);
        assertSame(client2, client3);

        client2.close();
        assertTrue(client3.isClosed());

    }

    @Test
    void testAutoPollRefreshFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        cl.forceRefresh();
        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    void testLazyRefreshFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        cl.forceRefresh();
        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    void testManualPollRefreshFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        cl.forceRefresh();
        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    void testAutoPollUserAgentHeader() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.autoPoll(2));
            options.baseUrl(server.url("/").toString());
        });

        cl.forceRefresh();

        assertEquals("ConfigCat-Java/a-" + Constants.VERSION, server.takeRequest().getHeader("X-ConfigCat-UserAgent"));

        server.shutdown();
        cl.close();
    }

    @Test
    void testLazyUserAgentHeader() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.lazyLoad(2));
            options.baseUrl(server.url("/").toString());
        });

        cl.forceRefresh();

        assertEquals("ConfigCat-Java/l-" + Constants.VERSION, server.takeRequest().getHeader("X-ConfigCat-UserAgent"));

        server.shutdown();
        cl.close();
    }

    @Test
    void testManualAgentHeader() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        cl.forceRefresh();

        assertEquals("ConfigCat-Java/m-" + Constants.VERSION, server.takeRequest().getHeader("X-ConfigCat-UserAgent"));

        server.shutdown();
        cl.close();
    }

    @Test
    void testOnlineOffline() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        assertFalse(cl.isOffline());

        cl.forceRefresh();

        assertEquals(1, server.getRequestCount());

        cl.setOffline();
        assertTrue(cl.isOffline());

        cl.forceRefresh();

        assertEquals(1, server.getRequestCount());

        cl.setOnline();
        cl.forceRefresh();

        assertEquals(2, server.getRequestCount());

        server.shutdown();
        cl.close();
    }

    @Test
    void testInitOffline() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
            options.offline(true);
        });

        assertTrue(cl.isOffline());

        cl.forceRefresh();

        assertEquals(0, server.getRequestCount());

        cl.setOnline();
        cl.forceRefresh();

        assertEquals(1, server.getRequestCount());

        server.shutdown();
        cl.close();
    }

    @Test
    void testHooks() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(RULES_JSON));
        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        AtomicBoolean changed = new AtomicBoolean(false);
        AtomicBoolean ready = new AtomicBoolean(false);
        AtomicReference<String> error = new AtomicReference<>("");

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
            options.hooks().addOnConfigChanged(map -> changed.set(true));
            options.hooks().addOnClientReady(() -> ready.set(true));
            options.hooks().addOnError(error::set);
        });

        cl.forceRefresh();
        cl.forceRefresh();

        assertTrue(changed.get());
        assertTrue(ready.get());
        assertEquals("Unexpected HTTP response was received while trying to fetch config JSON: 500 Server Error", error.get());

        server.shutdown();
        cl.close();
    }

    @Test
    void testHooksSub() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(RULES_JSON));
        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        AtomicBoolean changed = new AtomicBoolean(false);
        AtomicReference<String> error = new AtomicReference<>("");

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
        });

        cl.getHooks().addOnConfigChanged(map -> changed.set(true));
        cl.getHooks().addOnError(error::set);

        cl.forceRefresh();
        cl.forceRefresh();

        assertTrue(changed.get());
        assertEquals("Unexpected HTTP response was received while trying to fetch config JSON: 500 Server Error", error.get());

        server.shutdown();
        cl.close();
    }

    @Test
    void testHooksAutoPollSub() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(200).setBody(RULES_JSON));
        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        AtomicBoolean changed = new AtomicBoolean(false);
        AtomicBoolean ready = new AtomicBoolean(false);
        AtomicReference<String> error = new AtomicReference<>("");

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.autoPoll());
            options.baseUrl(server.url("/").toString());
        });

        cl.getHooks().addOnConfigChanged(map -> changed.set(true));
        cl.getHooks().addOnClientReady(() -> ready.set(true));
        cl.getHooks().addOnError(error::set);

        cl.forceRefresh();
        cl.forceRefresh();

        assertTrue(changed.get());
        assertTrue(ready.get());
        assertEquals("Unexpected HTTP response was received while trying to fetch config JSON: 500 Server Error", error.get());

        server.shutdown();
        cl.close();
    }

    @Test
    void testOnFlagEvaluationError() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        AtomicBoolean called = new AtomicBoolean(false);

        ConfigCatClient cl = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.pollingMode(PollingModes.lazyLoad());
            options.baseUrl(server.url("/").toString());
            options.hooks().addOnFlagEvaluated(details -> {
                assertEquals("", details.getValue());
                assertEquals("Config JSON is not present when evaluating setting 'key'. Returning the `defaultValue` parameter that you specified in your application: ''.", details.getError());
                assertTrue(details.isDefaultValue());
                called.set(true);
            });
        });

        cl.getValue(String.class, "key", "");
        assertTrue(called.get());

        server.shutdown();
        cl.close();
    }

    @Test
    void testCacheKey() throws NoSuchFieldException, IllegalAccessException, IOException {
        //Test Data: SDKKey "configcat-sdk-1/TEST_KEY-0123456789012/1234567890123456789012", HASH "f83ba5d45bceb4bb704410f51b704fb6dfa19942"
        ConfigCatClient clTest1 = ConfigCatClient.get("configcat-sdk-1/TEST_KEY-0123456789012/1234567890123456789012");

        String test1SdkKeyCacheKeyWithReflection = getCacheKeyWithReflection(clTest1);
        assertEquals("f83ba5d45bceb4bb704410f51b704fb6dfa19942", test1SdkKeyCacheKeyWithReflection);

        //Test Data: SDKKey "configcat-sdk-1/TEST_KEY2-123456789012/1234567890123456789012", HASH "da7bfd8662209c8ed3f9db96daed4f8d91ba5876"
        ConfigCatClient clTest2 = ConfigCatClient.get("configcat-sdk-1/TEST_KEY2-123456789012/1234567890123456789012");

        String test2SdkKeyCacheKeyWithReflection = getCacheKeyWithReflection(clTest2);
        assertEquals("da7bfd8662209c8ed3f9db96daed4f8d91ba5876", test2SdkKeyCacheKeyWithReflection);

        ConfigCatClient.closeAll();
    }

    private static String getCacheKeyWithReflection(ConfigCatClient cl) throws NoSuchFieldException, IllegalAccessException {
        Field configServiceField = ConfigCatClient.class.getDeclaredField("configService");
        configServiceField.setAccessible(true);

        ConfigService configService = (ConfigService) configServiceField.get(cl);

        Field cacheKeyField = ConfigService.class.getDeclaredField("cacheKey");
        cacheKeyField.setAccessible(true);

        return (String) cacheKeyField.get(configService);
    }
}