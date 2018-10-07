package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigCatClientIntegrationTest {

    private static final String APIKEY = "TEST_KEY";
    private ConfigCatClient client;
    private MockWebServer server;

    private static final String TEST_JSON = "{ fakeKey: { Value: %s, RolloutPercentageItems: [] ,RolloutRules: [] } }";
    private static final String TEST_OBJECT_JSON = "{ value1: { Value: 1, RolloutPercentageItems: [] ,RolloutRules: [] }, " +
                                                    " value2: { Value: \"abc\", RolloutPercentageItems: [] ,RolloutRules: [] }, " +
                                                    " value3: { Value: 2.4, RolloutPercentageItems: [] ,RolloutRules: [] }," +
                                                    " value4: { Value: true, RolloutPercentageItems: [] ,RolloutRules: [] }}";

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.client = ConfigCatClient.newBuilder()
                .httpClient(new OkHttpClient.Builder().build())
                .refreshPolicy((configFetcher, cache) -> {
                    configFetcher.setUrl(this.server.url("/").toString());
                    return ExpiringCachePolicy.newBuilder()
                            .cacheRefreshIntervalInSeconds(2)
                            .asyncRefresh(true)
                            .build(configFetcher, cache);
                })
                .build(APIKEY);
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.client.close();
        this.server.shutdown();
    }

    @Test
    public void getConfiguration() {
        Sample sample = new Sample();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_OBJECT_JSON));

        Sample result = this.client.getConfiguration(Sample.class,null);
        assertEquals(sample.value1, result.value1);
        assertEquals(sample.value2, result.value2);
        assertEquals(sample.value3, result.value3);
        assertEquals(sample.value4, result.value4);
    }

    @Test
    public void getConfigurationReturnsDefaultOnFail() {
        Sample sample = new Sample();
        server.enqueue(new MockResponse().setResponseCode(500));

        Sample result = this.client.getConfiguration(Sample.class,sample);
        assertSame(sample, result);
    }

    @Test
    public void getConfigurationReturnsDefaultOnException() {
        String badJson = "{ test: test] }";
        Sample def = new Sample();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson));

        Sample result = this.client.getConfiguration(Sample.class,def);
        assertSame(def, result);
    }

    @Test
    public void getConfigurationReturnsDefaultOnExceptionRepeatedly() {
        String badJson = "{ test: test] }";
        Sample def = new Sample();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("test").setBodyDelay(5, TimeUnit.SECONDS));

        Sample result = this.client.getConfiguration(Sample.class,def);
        assertSame(def, result);
    }

    @Test
    public void getStringValue() {
        String sValue = "ááúúóüüőőööúúűű";
        String result = String.format(TEST_JSON, sValue);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        String config = this.client.getValue(String.class,"fakeKey", null);
        assertEquals(sValue, config);
    }

    @Test
    public void getStringValueReturnsDefaultOnFail() {
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(500));
        String config = this.client.getValue(String.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getStringValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        String config = this.client.getValue(String.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getBooleanValue() {
        String result = String.format(TEST_JSON, "true");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getValue(Boolean.class,"fakeKey", false);
        assertTrue(config);
    }

    @Test
    public void getBooleanValueReturnsDefaultOnFail() {
        server.enqueue(new MockResponse().setResponseCode(500));
        boolean config = this.client.getValue(Boolean.class,"fakeKey", true);
        assertTrue(config);
    }

    @Test
    public void getBooleanValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        boolean def = true;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getValue(Boolean.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getIntegerValue() {
        int iValue = 342423;
        String result = String.format(TEST_JSON, String.valueOf(iValue));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        int config = this.client.getValue(Integer.class,"fakeKey", 0);
        assertEquals(iValue, config);
    }

    @Test
    public void getIntegerValueReturnsDefaultOnFail() {
        int def = 342423;
        server.enqueue(new MockResponse().setResponseCode(500));
        int config = this.client.getValue(Integer.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getIntegerValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        int def = 14;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        int config = this.client.getValue(Integer.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getDoubleValue() {
        double iValue = 432.234;
        String result = String.format(TEST_JSON, String.valueOf(iValue));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        double config = this.client.getValue(Double.class,"fakeKey", 0.0);
        assertEquals(iValue, config);
    }

    @Test
    public void getDoubleValueReturnsDefaultOnFail() {
        double def = 432.234;
        server.enqueue(new MockResponse().setResponseCode(500));
        double config = this.client.getValue(Double.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getDoubleValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        double def = 14.5;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        double config = this.client.getValue(Double.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void invalidateCache() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, String.valueOf("test"))));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, String.valueOf("test2"))));

        assertEquals("test", this.client.getValue(String.class, "fakeKey", null));
        this.client.forceRefresh();
        assertEquals("test2", this.client.getValue(String.class, "fakeKey", null));
    }

    @Test
    public void invalidateCacheFail() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, String.valueOf("test"))));
        server.enqueue(new MockResponse().setResponseCode(500));

        assertEquals("test", this.client.getValue(String.class, "fakeKey", null));
        this.client.forceRefresh();
        assertEquals("test", this.client.getValue(String.class, "fakeKey", null));
    }

    @Test
    public void getConfigurationJsonStringWithDefaultConfigTimeout() {
        ConfigCatClient cl = ConfigCatClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(APIKEY);

        // makes a call to a real url which would fail, null expected
        String config = cl.getValue(String.class, "test", null);
        assertEquals(null, config);
    }

    @Test
    public void getConfigurationJsonStringWithDefaultConfig() {
        ConfigCatClient cl = new ConfigCatClient(APIKEY);

        // makes a call to a real url which would fail, timeout expected
        assertThrows(TimeoutException.class, () -> cl.getValueAsync(String.class, "test", null).get(2, TimeUnit.SECONDS));
    }

    private static class Sample {
        static Sample Empty = new Sample();
        private int value1 = 1;
        private String value2 = "abc";
        private double value3 = 2.4;
        private boolean value4 = true;
    }
}