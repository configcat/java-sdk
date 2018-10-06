package websample;

import com.configcat.ConfigCatClient;
import com.configcat.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@Controller
public class ConfigController {

    @Autowired
    ConfigCatClient client;

    // reads the configuration value from the client
    @RequestMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public @ResponseBody String config() {

        // create a user object to identify the caller
        User user = User.newBuilder()
                .build("key");

        // get individual config values identified by a key for a user
        return this.client.getValue(String.class,"keySampleText", user, "") ;
    }

    // invalidates the config cache, this forces a refresh, can be used
    // with a configured webhook: https://configcat.com/Docs#integrations-webhooks
    @RequestMapping(value = "/configchanged", method = RequestMethod.POST)
    public void configchanged() {
        this.client.forceRefresh();
    }
}
