package websample;

import com.configcat.ConfigCatClient;
import com.configcat.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Controller
public class ConfigController {

    @Autowired
    ConfigCatClient client;

    @RequestMapping(value = "api/config/awesome", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public @ResponseBody
    String awesome() {
        return this.client.getValue(Boolean.class, "isAwesomeFeatureEnabled", false).toString();
    }

    @RequestMapping(value = "api/config/poc", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public @ResponseBody
    String poc(@RequestParam String email) {
        User user = User.newBuilder().email(email).build("#SOME-USER-ID#");
        return this.client.getValue(Boolean.class, "isPOCFeatureEnabled", user, false).toString();
    }
}
