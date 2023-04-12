import com.configcat.ConfigCatClient;
import com.configcat.LogLevel;
import com.configcat.User;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        ConfigCatClient client = ConfigCatClient.get("PKDVCLf-Hq-h-kCzMp-L7Q/HhOWfwVtZ0mb30i9wi17GQ", options -> {
            // Info level logging helps to inspect the feature flag evaluation process.
            // Use the default Warning level to avoid too detailed logging in your application.
            options.logLevel(LogLevel.DEBUG);
        });

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
