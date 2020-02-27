import com.configcat.ConfigCatClient;
import com.configcat.User;

public class Main {
    public static void main(String[] args) {
        ConfigCatClient client = ConfigCatClient
                .newBuilder()
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/HhOWfwVtZ0mb30i9wi17GQ");

        // get individual config values identified by a key for a user
        System.out.println("isAwesomeFeatureEnabled: " + client.getValue(String.class,"isAwesomeFeatureEnabled", ""));


        // create a user object to identify the caller
        User user = User.newBuilder()
                .email("configcat@example.com")
                .build("key");

        System.out.println("isPOCFeatureEnabled: " + client.getValue(String.class,"isPOCFeatureEnabled", user, ""));
    }
}
