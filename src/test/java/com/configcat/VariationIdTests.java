package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class VariationIdTests {

    private static final String TEST_JSON = "{ 'p':{ 'u': 'https://cdn-global.configcat.com', 'r': '0 ', 's': 'test-salt'}, 'f':{ 'key1':{ 't':0, 'r':[ { 'c':[ { 'u':{ 'a': 'Email', 'c': 2 , 'l ':[ '@configcat.com' ] } } ], 's':{ 'v': { 'b':true }, 'i': 'rolloutId1' } }, { 'c': [ { 'u' :{ 'a': 'Email', 'c': 2, 'l' : [ '@test.com' ] } } ], 's' : { 'v' : { 'b': false }, 'i': 'rolloutId2' } } ], 'p':[ { 'p':50, 'v' : { 'b': true }, 'i' : 'percentageId1'  },  { 'p' : 50, 'v' : { 'b': false }, 'i': 'percentageId2' } ], 'v':{ 'b':true }, 'i': 'fakeId1' }, 'key2': { 't':0, 'v': { 'b': false }, 'i': 'fakeId2' }, 'key3': { 't': 0, 'r':[ { 'c': [ { 'u':{ 'a': 'Email', 'c':2,  'l':[ '@configcat.com' ] } } ], 'p': [{ 'p':50, 'v':{ 'b': true  }, 'i' : 'targetPercentageId1' },  { 'p': 50, 'v': { 'b':false }, 'i' : 'targetPercentageId2' } ] } ], 'v':{ 'b': false  }, 'i': 'fakeId3' } } }";
    private static final String TEST_JSON_INCORRECT = "{ 'p':{ 'u': 'https://cdn-global.configcat.com', 'r': '0 ', 's': 'test-salt' }, 'f' :{ 'incorrect' : { 't': 0, 'r': [ {'c': [ {'u': {'a': 'Email', 'c': 2, 'l': ['@configcat.com'] } } ] } ],'v': {'b': false}, 'i': 'incorrectId' } } }";
    private ConfigCatClient client;
    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.client = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.httpClient(new OkHttpClient.Builder().build());
            options.pollingMode(PollingModes.lazyLoad(2));
            options.baseUrl(this.server.url("/").toString());
        });
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.client.close();
        this.server.shutdown();
    }

    @Test
    public void getVariationIdWorks() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        EvaluationDetails<Boolean> valueDetails = client.getValueDetails(Boolean.class, "key1", null);
        assertEquals("fakeId1", valueDetails.getVariationId());
    }

    @Test
    public void getVariationIdNotFound() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        EvaluationDetails<Boolean> valueDetails = client.getValueDetails(Boolean.class, "nonexisting", false);
        assertEquals("", valueDetails.getVariationId());
    }

    @Test
    public void getAllVariationIdsWorks() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));

        List<EvaluationDetails<Object>> allValueDetails = client.getAllValueDetails(null);
        assertEquals(3, allValueDetails.size());
        assertEquals("fakeId1", allValueDetails.get(0).getVariationId());
        assertEquals("fakeId2", allValueDetails.get(1).getVariationId());
        assertEquals("fakeId3", allValueDetails.get(2).getVariationId());
    }

    @Test
    public void getAllVariationIdsWorksEmpty() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        List<EvaluationDetails<Object>> allValueDetails = client.getAllValueDetails(null);
        assertEquals(0, allValueDetails.size());
    }

    @Test
    public void getKeyAndValueWorks() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        Map.Entry<String, Boolean> result = client.getKeyAndValue(boolean.class, "fakeId2");
        assertEquals("key2", result.getKey());
        assertFalse(result.getValue());

        Map.Entry<String, Boolean> result2 = client.getKeyAndValue(boolean.class, "percentageId2");
        assertEquals("key1", result2.getKey());
        assertFalse(result2.getValue());

        Map.Entry<String, Boolean> result3 = client.getKeyAndValue(boolean.class, "rolloutId1");
        assertEquals("key1", result3.getKey());
        assertTrue(result3.getValue());

        Map.Entry<String, Boolean> result4 = client.getKeyAndValue(boolean.class, "targetPercentageId2");
        assertEquals("key3", result4.getKey());
        assertFalse(result4.getValue());

    }

    @Test
    public void getKeyAndValueAsyncWorks() throws ExecutionException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        Map.Entry<String, Boolean> result = client.getKeyAndValueAsync(boolean.class, "fakeId1").get();
        assertEquals("key1", result.getKey());
        assertTrue(result.getValue());
    }

    @Test
    public void getKeyAndValueNotFound() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        Map.Entry<String, Boolean> result = client.getKeyAndValue(boolean.class, "nonexisting");
        assertNull(result);
    }

    @Test
    public void getKeyAndValueIncorrectTargetingRule() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_INCORRECT));
        Map.Entry<String, Boolean> result = client.getKeyAndValue(boolean.class, "targetPercentageId2");
        assertNull(result);
    }
}
