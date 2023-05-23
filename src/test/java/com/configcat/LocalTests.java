package com.configcat;

import com.configcat.override.OverrideBehaviour;
import com.configcat.polling.PollingModes;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LocalTests {
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";

    @Test
    public void withClient() throws IOException {
        ConfigCatClient client = ConfigCatClient.get("localhost", options ->
                options.flagOverrides(OverrideDataSourceBuilder.classPathResource("test.json"), OverrideBehaviour.LOCAL_ONLY));

        assertTrue(client.getValue(Boolean.class, "enabledFeature", User.newBuilder().build("test"), false));
        assertFalse(client.getValue(Boolean.class, "disabledFeature", User.newBuilder().build("test"), true));
        assertEquals(5, (int) client.getValue(Integer.class, "intSetting", User.newBuilder().build("test"), 0));
        assertEquals(3.14, (double) client.getValue(Double.class, "doubleSetting", User.newBuilder().build("test"), 0.0));
        assertEquals("test", client.getValue(String.class, "stringSetting", User.newBuilder().build("test"), ""));

        client.close();
    }

    @Test
    public void withClient_Simple() throws IOException {

        ConfigCatClient client = ConfigCatClient.get("localhost", options ->
                options.flagOverrides(OverrideDataSourceBuilder.classPathResource("test-simple.json"), OverrideBehaviour.LOCAL_ONLY));

        assertTrue(client.getValue(Boolean.class, "enabledFeature", User.newBuilder().build("test"), false));
        assertFalse(client.getValue(Boolean.class, "disabledFeature", User.newBuilder().build("test"), true));
        assertEquals(5, (int) client.getValue(Integer.class, "intSetting", User.newBuilder().build("test"), 0));
        assertEquals(3.14, (double) client.getValue(Double.class, "doubleSetting", User.newBuilder().build("test"), 0.0));
        assertEquals("test", client.getValue(String.class, "stringSetting", User.newBuilder().build("test"), ""));

        client.close();
    }

    @Test
    public void object() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("enabledFeature", true);
        map.put("disabledFeature", false);
        map.put("intSetting", 5);
        map.put("doubleSetting", 3.14);
        map.put("stringSetting", "test");

        ConfigCatClient client = ConfigCatClient.get("localhost", options -> options.flagOverrides(OverrideDataSourceBuilder.map(map), OverrideBehaviour.LOCAL_ONLY));

        assertTrue(client.getValue(Boolean.class, "enabledFeature", User.newBuilder().build("test"), false));
        assertFalse(client.getValue(Boolean.class, "disabledFeature", User.newBuilder().build("test"), true));
        assertEquals(5, (int) client.getValue(Integer.class, "intSetting", User.newBuilder().build("test"), 0));
        assertEquals(3.14, (double) client.getValue(Double.class, "doubleSetting", User.newBuilder().build("test"), 0.0));
        assertEquals("test", client.getValue(String.class, "stringSetting", User.newBuilder().build("test"), ""));

        client.close();
    }

    @Test
    public void reload() throws IOException, InterruptedException {
        File newFile = new File("src/test/resources/auto_created.txt");
        if (newFile.createNewFile()) {
            try {
                this.writeContent(newFile, String.format(TEST_JSON, "test"));

                ConfigCatClient client = ConfigCatClient.get("localhost", options ->
                        options.flagOverrides(OverrideDataSourceBuilder.localFile("src/test/resources/auto_created.txt", true), OverrideBehaviour.LOCAL_ONLY));

                assertEquals("test", client.getValue(String.class, "fakeKey", ""));
                this.writeContent(newFile, String.format(TEST_JSON, "modified"));
                Thread.sleep(500);
                assertEquals("modified", client.getValue(String.class, "fakeKey", ""));
                client.close();
            } finally {
                newFile.delete();
            }
        } else {
            fail("The test wasn't able to create the test file.");
        }
    }

    @Test
    public void localOverRemote() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        Map<String, Object> map = new HashMap<>();
        map.put("fakeKey", true);
        map.put("nonexisting", true);

        ConfigCatClient client = ConfigCatClient.get("localhost", options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
            options.flagOverrides(OverrideDataSourceBuilder.map(map), OverrideBehaviour.LOCAL_OVER_REMOTE);
        });

        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, false)));

        client.forceRefresh();
        assertTrue(client.getValue(Boolean.class, "fakeKey", false));
        assertTrue(client.getValue(Boolean.class, "nonexisting", false));

        server.close();
        client.close();
    }

    @Test
    public void remoteOverLocal() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        Map<String, Object> map = new HashMap<>();
        map.put("fakeKey", true);
        map.put("nonexisting", true);

        ConfigCatClient client = ConfigCatClient.get("localhost", options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.baseUrl(server.url("/").toString());
            options.flagOverrides(OverrideDataSourceBuilder.map(map), OverrideBehaviour.REMOTE_OVER_LOCAL);
        });

        server.enqueue(new MockResponse().setResponseCode(200).setBody(String.format(TEST_JSON, false)));

        client.forceRefresh();
        assertFalse(client.getValue(Boolean.class, "fakeKey", false));
        assertTrue(client.getValue(Boolean.class, "nonexisting", false));

        server.close();
        client.close();
    }

    private void writeContent(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();
    }
}
