package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigCatClientTest {

    private static final String APIKEY = "TEST_KEY";

    private static final String TEST_JSON = "{ f: { fakeKey: { v: fakeValue, s: 0, p: [] ,r: [] } } }";
    private static final String TEST_JSON_MULTIPLE = "{ f: { key1: { v: true, i: 'fakeId1', p: [] ,r: [] }, key2: { v: false, i: 'fakeId2', p: [] ,r: [] } } }";
    public static final String TEST_JSON_DEFAULT_USER = "{'f':{'fakeKey':{'v':'defaultValue','i':'defaultId', 'r':[{'o':'0','a':'Identifier','t':2,'c':'test1','v':'fakeValue1','i':'test1Id'},{'o':'1','a':'Identifier','t':2,'c':'test2','v':'fakeValue2','i':'test2Id'}]}}}";

    @Test
    public void ensuresApiKeyIsNotNull() {
        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.get(null));

        assertEquals("'sdkKey' cannot be null or empty.", builderException.getMessage());
    }

    @Test
    public void ensuresApiKeyIsNotEmpty() {
        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.get(""));

        assertEquals("'sdkKey' cannot be null or empty.", builderException.getMessage());
    }

    @Test
    public void getValueWithDefaultConfigTimeout() throws IOException {
        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .httpClient(new OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build());
        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

        // makes a call to a real url which would fail, default expected
        boolean config = cl.getValue(Boolean.class, "key", true);
        assertTrue(config);

        cl.close();
    }

    @Test
    public void getConfigurationWithFailingCache() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .cache(new FailingCache())
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .cache(new FailingCache())
                .mode(PollingModes.autoPoll(5))
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationExpCacheFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .cache(new FailingCache())
                .mode(PollingModes.lazyLoad(5))
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationManualFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .cache(new FailingCache())
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        assertEquals("", cl.getValue(String.class, "fakeKey", ""));

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationReturnsPreviousCachedOnTimeout() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString())
                .httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.autoPoll(60, 1))
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString())
                .httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString())
                .httpClient(new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("test").setBodyDelay(3, TimeUnit.SECONDS));

        cl.forceRefresh();

        server.shutdown();
        cl.close();
    }

    @Test
    public void getAllValues() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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
        ConfigCatClient client = ConfigCatClient.get("key");
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class, null, false));
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class, "", false));

        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class, null, false).get());
        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class, "", false).get());
    }

    @Test
    public void testDefaultUser() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        User user1 = User.newBuilder().build("test1");
        User user2 = User.newBuilder().build("test2");

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString())
                .defaultUser(user2);

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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
    void testSingleton() throws IOException {
        ConfigCatClient client1 = ConfigCatClient.get("test");
        ConfigCatClient client2 = ConfigCatClient.get("test");

        assertSame(client1, client2);

        ConfigCatClient.closeAll();

        client1 = ConfigCatClient.get("test");

        assertNotSame(client1, client2);
    }

    @Test
    void testSingletonOptions() throws IOException {
        ConfigCatClient.Options client1Options = new ConfigCatClient.Options()
                .mode(PollingModes.autoPoll(60));
        ConfigCatClient client1 = ConfigCatClient.get("test", client1Options);
        ConfigCatClient.Options client2Options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll());
        ConfigCatClient client2 = ConfigCatClient.get("test", client2Options);

        assertSame(client1, client2);

        ConfigCatClient.closeAll();
    }

    @Test
    void testClose() throws IOException {
        ConfigCatClient client1 = ConfigCatClient.get("test");
        assertFalse(client1.isClosed());
        client1.close();
        assertTrue(client1.isClosed());
    }

    @Test
    void testSingletonCloseAffects() throws IOException {
        ConfigCatClient client1 = ConfigCatClient.get("test");
        client1.close();
        assertTrue(client1.isClosed());

        ConfigCatClient client2 = ConfigCatClient.get("test");
        assertNotSame(client1, client2);
        client1.close();
        assertFalse(client2.isClosed());

        ConfigCatClient client3 = ConfigCatClient.get("test");
        assertSame(client2, client3);

        client2.close();
        assertTrue(client3.isClosed());

    }

    @Test
    void testAutoPollRefreshFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(new MockResponse().setResponseCode(500).setBody(""));

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.autoPoll(2))
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.lazyLoad(2))
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString());

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

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

        ConfigCatClient.Options options = new ConfigCatClient.Options()
                .mode(PollingModes.manualPoll())
                .baseUrl(server.url("/").toString())
                .offline(true);

        ConfigCatClient cl = ConfigCatClient.get(APIKEY, options);

        assertTrue(cl.isOffline());

        cl.forceRefresh();

        assertEquals(0, server.getRequestCount());

        cl.setOnline();
        cl.forceRefresh();

        assertEquals(1, server.getRequestCount());

        server.shutdown();
        cl.close();
    }
}