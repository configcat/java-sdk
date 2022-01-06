package websample;

import com.configcat.ConfigCatClient;
import com.configcat.LogLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean
    public ConfigCatClient configCatClient() {
        return ConfigCatClient
                .newBuilder()
                // Info level logging helps to inspect the feature flag evaluation process.
                // Use the default Warning level to avoid too detailed logging in your application.
                .logLevel(LogLevel.INFO)
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/HhOWfwVtZ0mb30i9wi17GQ");
    }
}
