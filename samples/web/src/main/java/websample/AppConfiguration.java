package websample;

import com.configcat.ConfigCatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean
    public ConfigCatClient configCatClient() {
        return ConfigCatClient
                .newBuilder()
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A");
    }
}
