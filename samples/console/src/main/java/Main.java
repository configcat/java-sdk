import com.configcat.ConfigCatClient;
import com.configcat.User;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");

        ConfigCatClient client = ConfigCatClient.newBuilder()
                // Info level logging helps to inspect the feature flag evaluation process.
                // Use the default Warning level to avoid too detailed logging in your application.
                .logLevel(LogLevel.INFO)
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/HhOWfwVtZ0mb30i9wi17GQ");

        // Get individual config values identified by a key for a user.
        System.out.println("isAwesomeFeatureEnabled: " + client.getValue(String.class, "isAwesomeFeatureEnabled", ""));


        // Create a user object to identify the caller.
        User user = User.newBuilder()
                .email("configcat@example.com")
                .build("key");

        System.out.println("isPOCFeatureEnabled: " + client.getValue(String.class, "isPOCFeatureEnabled", user, ""));

        client.close();
    }
}
