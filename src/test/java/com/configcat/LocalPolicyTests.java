package com.configcat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class LocalPolicyTests {
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(LocalPolicyTests.class));

    @Test
    public void readFile() throws Exception {
        LocalFilePolicy policy = new LocalFilePolicy((LocalFilePollingMode) PollingModes.localClassPathResource("test.json", false), logger);
        assertNotNull(policy.getConfigurationAsync().get());
    }

    @Test
    public void withClient() throws IOException {
        ConfigCatClient client = new ConfigCatClient.Builder()
                .mode(PollingModes.localClassPathResource("test.json", false))
                .build("localhost");

        assertTrue(client.getValue(Boolean.class, "enabledFeature", User.newBuilder().build("test"), false));
        assertFalse(client.getValue(Boolean.class, "disabledFeature", User.newBuilder().build("test"), true));
        assertEquals(5, (int)client.getValue(Integer.class, "intSetting", User.newBuilder().build("test"), 0));
        assertEquals(3.14, (double)client.getValue(Double.class, "doubleSetting", User.newBuilder().build("test"), 0.0));
        assertEquals("test", client.getValue(String.class, "stringSetting", User.newBuilder().build("test"), ""));

        client.close();
    }

    @Test
    public void withClient_Simple() throws IOException {
        ConfigCatClient client = new ConfigCatClient.Builder()
                .mode(PollingModes.localClassPathResource("test-simple.json", false))
                .build("localhost");

        assertTrue(client.getValue(Boolean.class, "enabledFeature", User.newBuilder().build("test"), false));
        assertFalse(client.getValue(Boolean.class, "disabledFeature", User.newBuilder().build("test"), true));
        assertEquals(5, (int)client.getValue(Integer.class, "intSetting", User.newBuilder().build("test"), 0));
        assertEquals(3.14, (double)client.getValue(Double.class, "doubleSetting", User.newBuilder().build("test"), 0.0));
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
        ConfigCatClient client = new ConfigCatClient.Builder()
                .mode(PollingModes.localObject(map))
                .build("localhost");

        assertTrue(client.getValue(Boolean.class, "enabledFeature", User.newBuilder().build("test"), false));
        assertFalse(client.getValue(Boolean.class, "disabledFeature", User.newBuilder().build("test"), true));
        assertEquals(5, (int)client.getValue(Integer.class, "intSetting", User.newBuilder().build("test"), 0));
        assertEquals(3.14, (double)client.getValue(Double.class, "doubleSetting", User.newBuilder().build("test"), 0.0));
        assertEquals("test", client.getValue(String.class, "stringSetting", User.newBuilder().build("test"), ""));

        client.close();
    }

    @Test
    public void reload() throws IOException, ExecutionException, InterruptedException {
        File newFile = new File("src/test/resources/auto_created.txt");
        if (newFile.createNewFile()) {
            try {
                this.writeContent(newFile, String.format(TEST_JSON, "test"));
                LocalFilePolicy policy = new LocalFilePolicy((LocalFilePollingMode) PollingModes.localFile("src/test/resources/auto_created.txt", true), logger);
                assertEquals("test", policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
                this.writeContent(newFile, String.format(TEST_JSON, "modified"));
                Thread.sleep(500);
                assertEquals("modified", policy.getConfigurationAsync().get().entries.get("fakeKey").value.getAsString());
                policy.close();
            } finally {
                newFile.delete();
            }
        } else {
            fail("The test wasn't able to create the test file.");
        }
    }

    private void writeContent(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();
    }
}
