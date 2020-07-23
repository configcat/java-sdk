package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class VariationIdTests {

    private static final String TEST_JSON = "{ key1: { v: true, i: 'fakeId1', p: [] ,r: [] }, key2: { v: false, i: 'fakeId2', p: [] ,r: [] } }";
    private ConfigCatClient client;
    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.client = ConfigCatClient.newBuilder()
                .httpClient(new OkHttpClient.Builder().build())
                .mode(PollingModes.LazyLoad(2, false))
                .baseUrl(this.server.url("/").toString())
                .build("TEST_KEY");
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.client.close();
        this.server.shutdown();
    }

    @Test
    public void nullKeyThrows() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> client.getVariationId(null, null));

        assertEquals("key is null or empty", exception.getMessage());
    }

    @Test
    public void getVariationIdWorks() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        String result = client.getVariationId("key1", null);
        assertEquals("fakeId1", result);
    }

    @Test
    public void getVariationIdAsyncWorks() throws ExecutionException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        String result = client.getVariationIdAsync("key2", null).get();
        assertEquals("fakeId2", result);
    }

    @Test
    public void getVariationIdNotFound() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        String result = client.getVariationId("nonexisting", "defaultId");
        assertEquals("defaultId", result);
    }

    @Test
    public void getAllVariationIdsWorks() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        String[] result = client.getAllVariationIds().toArray(new String[0]);
        assertEquals(2, result.length);
        assertEquals("fakeId1", result[0]);
        assertEquals("fakeId2", result[1]);
    }

    @Test
    public void getAllVariationIdsEmpty() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        String[] result = client.getAllVariationIds().toArray(new String[0]);
        assertEquals(0, result.length);
    }

    @Test
    public void getAllVariationIdsAsyncWorks() throws ExecutionException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        String[] result = client.getAllVariationIdsAsync().get().toArray(new String[0]);
        assertEquals(2, result.length);
        assertEquals("fakeId1", result[0]);
        assertEquals("fakeId2", result[1]);
    }

    @Test
    public void getKeyAndValueWorks() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON));
        Map.Entry<String, Boolean> result = client.getKeyAndValue(boolean.class, "fakeId2");
        assertEquals("key2", result.getKey());
        assertFalse(result.getValue());
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
}
