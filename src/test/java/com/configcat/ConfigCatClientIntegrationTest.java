package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigCatClientIntegrationTest {

    private ConfigCatClient client;
    private MockWebServer server;

    private static final String TEST_JSON_STRING_VALUE = "{p: {s: 'test-salt' }, f: { fakeKey: { t: 1, v: {s: %s}, p: [] ,r: [] } } }";
    private static final String TEST_JSON_BOOLEAN_VALUE = "{p: {s: 'test-salt' }, f: { fakeKey: { t: 0, v: {b: %s}, p: [] ,r: [] } } }";
    private static final String TEST_JSON_INT_VALUE = "{p: {s: 'test-salt' }, f: { fakeKey: { t: 2, v: {i: %d}, p: [] ,r: [] } } }";
    private static final String TEST_JSON_DOUBLE_VALUE = "{p: {s: 'test-salt' }, f: { fakeKey: { t: 3, v: {d: %.3f}, p: [] ,r: [] } } }";

    @BeforeEach
    void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.client = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.httpClient(new OkHttpClient.Builder().build());
            options.pollingMode(PollingModes.lazyLoad(2));
            options.baseUrl(this.server.url("/").toString());
        });

    }

    @AfterEach
    void tearDown() throws IOException {
        ConfigCatClient.closeAll();
        this.server.shutdown();
    }

    @Test
    void getStringValue() {
        String sValue = "ááúúóüüőőööúúűű";
        String result = String.format(TEST_JSON_STRING_VALUE, sValue);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        String config = this.client.getValue(String.class, "fakeKey", null);
        assertEquals(sValue, config);
    }

    @Test
    void getStringValueReturnsDefaultOnFail() {
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(500));
        String config = this.client.getValue(String.class, "fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    void getStringValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        String config = this.client.getValue(String.class, "fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    void getBooleanValue() {
        String result = String.format(TEST_JSON_BOOLEAN_VALUE, "true");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getValue(Boolean.class, "fakeKey", false);
        assertTrue(config);
    }

    @Test
    void getBooleanValuePrimitive() {
        String result = String.format(TEST_JSON_BOOLEAN_VALUE, "true");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getValue(boolean.class, "fakeKey", false);
        assertTrue(config);
    }

    @Test
    void getBooleanValueReturnsDefaultOnFail() {
        server.enqueue(new MockResponse().setResponseCode(500));
        boolean config = this.client.getValue(boolean.class, "fakeKey", true);
        assertTrue(config);
    }

    @Test
    void getBooleanValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        boolean def = true;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getValue(Boolean.class, "fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    void getIntegerValue() {
        int iValue = 342423;
        String result = String.format(TEST_JSON_INT_VALUE, iValue);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        int config = this.client.getValue(Integer.class, "fakeKey", 0);
        assertEquals(iValue, config);
    }

    @Test
    void getIntegerValuePrimitive() {
        int iValue = 342423;
        String result = String.format(TEST_JSON_INT_VALUE, iValue);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        int config = this.client.getValue(int.class, "fakeKey", 0);
        assertEquals(iValue, config);
    }

    @Test
    void getIntegerValueReturnsDefaultOnFail() {
        int def = 342423;
        server.enqueue(new MockResponse().setResponseCode(500));
        int config = this.client.getValue(int.class, "fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    void getIntegerValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        int def = 14;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        int config = this.client.getValue(Integer.class, "fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    void getDoubleValue() {
        double dValue = 432.234;
        String result = String.format(Locale.US, TEST_JSON_DOUBLE_VALUE, dValue);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        double config = this.client.getValue(double.class, "fakeKey", 0.0);
        assertEquals(dValue, config);
    }

    @Test
    void getDoubleValueReturnsDefaultOnFail() {
        double def = 432.234;
        server.enqueue(new MockResponse().setResponseCode(500));
        double config = this.client.getValue(Double.class, "fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    void getDefaultValueWhenKeyNotExist() {
        String result = String.format(TEST_JSON_BOOLEAN_VALUE, "true");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getValue(Boolean.class, "nonExistingKey", false);
        assertFalse(config);
    }

    @Test
    void getDoubleValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        double def = 14.5;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        double config = this.client.getValue(Double.class, "fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    void invalidateCache() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON_STRING_VALUE, "test")));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON_STRING_VALUE, "test2")));

        assertEquals("test", this.client.getValue(String.class, "fakeKey", null));
        this.client.forceRefresh();
        assertEquals("test2", this.client.getValue(String.class, "fakeKey", null));
    }

    @Test
    void invalidateCacheFail() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON_STRING_VALUE, "test")));
        server.enqueue(new MockResponse().setResponseCode(500));

        assertEquals("test", this.client.getValue(String.class, "fakeKey", null));
        this.client.forceRefresh();
        assertEquals("test", this.client.getValue(String.class, "fakeKey", null));
    }

    @Test
    void getConfigurationJsonStringWithDefaultConfigTimeout() {
        ConfigCatClient cl = ConfigCatClient.get("configcat-sdk-1/TEST_KEY1-123456789012/1234567890123456789012", options -> options.httpClient(new OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()));

        // makes a call to a real url which would fail, null expected
        String config = cl.getValue(String.class, "test", null);
        assertNull(config);
    }

    @Test
    void getConfigurationJsonStringWithDefaultConfig() throws InterruptedException, ExecutionException, TimeoutException {
        ConfigCatClient cl = ConfigCatClient.get("configcat-sdk-1/TEST_KEY-DEFAULT-89012/1234567890123456789012");
        assertNull(cl.getValueAsync(String.class, "test", null).get(2, TimeUnit.SECONDS));
    }

    @Test
    void getAllKeys() throws IOException {

        ConfigCatClient cl = ConfigCatClient.get("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A", options -> {
            options.logLevel(LogLevel.INFO);
            options.dataGovernance(DataGovernance.EU_ONLY);
        });

        Collection<String> keys = cl.getAllKeys();

        assertEquals(16, keys.size());
        assertTrue(keys.contains("stringDefaultCat"));

        cl.close();
    }

    @Test
    void testEvalDetails() {
        ConfigCatClient cl = ConfigCatClient.get("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A");

        User user = new User.Builder()
                .email("test@configcat.com")
                .build("test@configcat.com");
        EvaluationDetails<String> details = cl.getValueDetails(String.class, "stringContainsDogDefaultCat", user, "");
        assertEquals("stringContainsDogDefaultCat", details.getKey());
        assertEquals("Dog", details.getValue());
        assertFalse(details.isDefaultValue());
        assertNull(details.getError());
        assertEquals("d0cd8f06", details.getVariationId());
        //the target should have one condition
        assertEquals(1, details.getMatchedTargetingRule().getConditions().length);
        assertNull(details.getMatchedPercentageOption());

        Condition condition = details.getMatchedTargetingRule().getConditions()[0];
        assertEquals("Email", condition.getComparisonCondition().getComparisonAttribute());
        assertEquals(2, condition.getComparisonCondition().getComparator());
        assertEquals(1, condition.getComparisonCondition().getStringArrayValue().length);
        assertEquals("@configcat.com", condition.getComparisonCondition().getStringArrayValue()[0]);
        assertEquals(user.getIdentifier(), details.getUser().getIdentifier());
    }

    @Test
    void testEvalDetailsHook() {
        User user = new User.Builder()
                .email("test@configcat.com")
                .build("test@configcat.com");

        AtomicBoolean called = new AtomicBoolean(false);


        ConfigCatClient cl = ConfigCatClient.get("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A", options -> {
            options.hooks().addOnFlagEvaluated(details -> {
                assertEquals("stringContainsDogDefaultCat", details.getKey());
                assertEquals("Dog", details.getValue());
                assertFalse(details.isDefaultValue());
                assertNull(details.getError());
                assertEquals("d0cd8f06", details.getVariationId());

                assertEquals(1, details.getMatchedTargetingRule().getConditions().length);
                assertNull(details.getMatchedPercentageOption());

                Condition condition = details.getMatchedTargetingRule().getConditions()[0];
                assertEquals("Email", condition.getComparisonCondition().getComparisonAttribute());
                assertEquals(2, condition.getComparisonCondition().getComparator());
                assertEquals(1, condition.getComparisonCondition().getStringArrayValue().length);
                assertEquals("@configcat.com", condition.getComparisonCondition().getStringArrayValue()[0]);
                assertEquals(user.getIdentifier(), details.getUser().getIdentifier());
                called.set(true);
            });
        });

        cl.getValueDetails(String.class, "stringContainsDogDefaultCat", user, "");

        assertTrue(called.get());
    }
}