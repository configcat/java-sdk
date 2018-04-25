package com.configcat;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigCatClientTest {

    private static final String APIKEY = "TEST_KEY";

    @Test
    public void ensuresApiKeyIsNotNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new ConfigCatClient(null));

        assertEquals("apiKey is null or empty", exception.getMessage());

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.newBuilder().build(null));

        assertEquals("apiKey is null or empty", builderException.getMessage());
    }

    @Test
    public void ensuresApiKeyIsNotEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new ConfigCatClient(""));

        assertEquals("apiKey is null or empty", exception.getMessage());

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> ConfigCatClient.newBuilder().build(""));

        assertEquals("apiKey is null or empty", builderException.getMessage());
    }

    @Test
    public void ensuresMaxWaitTimeoutGreaterThanTwoSeconds() {
        assertThrows(IllegalArgumentException.class, () -> ConfigCatClient
                .newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(1));
    }

    @Test
    public void getConfigurationJsonWithDefaultConfigTimeout() {
        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(APIKEY);

        // makes a call to a real url which would fail, default expected
        ConfigCatClientIntegrationTest.Sample config = cl.getConfiguration(ConfigCatClientIntegrationTest.Sample.class, ConfigCatClientIntegrationTest.Sample.Empty);
        assertEquals(ConfigCatClientIntegrationTest.Sample.Empty, config);
    }

    @Test
    public void getValueWithDefaultConfigTimeout() throws IOException {
        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(APIKEY);

        // makes a call to a real url which would fail, default expected
        boolean config = cl.getValue(Boolean.class, "key", true);
        assertTrue(config);

        cl.close();
    }

    @Test
    public void getPolicy() {
        ConfigCatClient cl = new ConfigCatClient(APIKEY);
        assertNotNull(cl.getRefreshPolicy(AutoPollingPolicy.class));
    }

    @Test
    public void getConfigurationWithFailingCache() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .cache(new FailingCache())
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new ManualPollingPolicy(f,c);
                })
                .build(APIKEY);

        String result = "{ \"fakeKey\":\"fakeValue\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));

        assertEquals(result, cl.getConfigurationJsonString());

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationReturnsPreviousCachedOnFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new ManualPollingPolicy(f,c);
                })
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(APIKEY);

        String result = "{ \"fakeKey\":\"fakeValue\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("delayed").setBodyDelay(5, TimeUnit.SECONDS));

        assertEquals(result, cl.getConfigurationJsonString());
        assertEquals(result, cl.getConfigurationJsonString());

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationReturnsDefaultOnExceptionRepeatedly() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new ManualPollingPolicy(f,c);
                })
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(APIKEY);

        String badJson = "{ test: test] }";
        ConfigCatClientIntegrationTest.Sample def = new ConfigCatClientIntegrationTest.Sample();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson).setBodyDelay(5, TimeUnit.SECONDS));

        assertSame(def, cl.getConfiguration(ConfigCatClientIntegrationTest.Sample.class, def));

        assertSame(def, cl.getConfiguration(ConfigCatClientIntegrationTest.Sample.class, def));

        server.shutdown();
        cl.close();
    }

    @Test
    public void getValueReturnsDefaultOnExceptionRepeatedly() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new ManualPollingPolicy(f,c);
                })
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(APIKEY);

        String badJson = "{ test: test] }";
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson).setBodyDelay(5, TimeUnit.SECONDS));

        assertSame(def, cl.getValue(String.class, "test", def));

        assertSame(def, cl.getValue(String.class, "test", def));

        server.shutdown();
        cl.close();
    }

    @Test
    public void forceRefreshWithTimeout() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new ManualPollingPolicy(f,c);
                })
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(APIKEY);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("test").setBodyDelay(5, TimeUnit.SECONDS));

        cl.forceRefresh();

        server.shutdown();
        cl.close();
    }

    @Test
    public void getValueInvalidArguments() {
        ConfigCatClient client = new ConfigCatClient("key");
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class,null, false));
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class,"", false));
        assertThrows(IllegalArgumentException.class, () -> client.getValue(ConfigCatClientIntegrationTest.Sample.class,"key", ConfigCatClientIntegrationTest.Sample.Empty));

        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class,null, false).get());
        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class,"", false).get());
        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(ConfigCatClientIntegrationTest.Sample.class,"key", ConfigCatClientIntegrationTest.Sample.Empty).get());
    }
}