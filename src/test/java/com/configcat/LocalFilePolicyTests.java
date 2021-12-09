package com.configcat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class LocalFilePolicyTests {
    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, p: [] ,r: [] } } }";
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(LocalFilePolicyTests.class));

    @Test
    public void readFile() throws Exception {
        LocalPolicy policy = new LocalPolicy((LocalPollingMode)PollingModes.LocalClassPathResource("test.json", false));
        assertNotNull(policy.getConfigurationAsync().get());
    }

    @Test
    public void withClient() {
        ConfigCatClient client = new ConfigCatClient.Builder()
                .mode(PollingModes.LocalClassPathResource("test.json", false))
                .build("localhost");

        assertTrue(client.getValue(Boolean.class, "enabledFeature", User.newBuilder().build("test"), false));
        assertFalse(client.getValue(Boolean.class, "disabledFeature", User.newBuilder().build("test"), true));
    }

    @Test
    public void withClient_Simple() {
        ConfigCatClient client = new ConfigCatClient.Builder()
                .mode(PollingModes.LocalClassPathResource("test-simple.json", false))
                .build("localhost");

        assertTrue(client.getValue(Boolean.class, "enabledFeature", User.newBuilder().build("test"), false));
        assertFalse(client.getValue(Boolean.class, "disabledFeature", User.newBuilder().build("test"), true));
    }

    @Test
    public void reload() throws IOException, ExecutionException, InterruptedException {
        File newFile = new File("src/test/resources/auto_created.txt");
        newFile.createNewFile();
        try {
            this.writeContent(newFile, String.format(TEST_JSON, "test"));
            LocalPolicy policy = new LocalPolicy((LocalPollingMode)PollingModes.LocalFile("src/test/resources/auto_created.txt", true));
            assertEquals("test", policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());
            this.writeContent(newFile, String.format(TEST_JSON, "modified"));
            Thread.sleep(500);
            assertEquals("modified", policy.getConfigurationAsync().get().Entries.get("fakeKey").Value.getAsString());
            policy.close();
        } finally {
            newFile.delete();
        }
    }

    private void writeContent(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();
    }
}
