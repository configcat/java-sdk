import com.configcat.ConfigCatClient;
import com.configcat.User;

public class Main {
    public static void main(String[] args) {
        ConfigCatClient client = ConfigCatClient
                .newBuilder()
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A");

        // create a user object to identify the caller
        User user = User.newBuilder()
                .build("key");

        // get individual config values identified by a key for a user
        System.out.println("keySampleText: " + client.getValue(String.class,"keySampleText", user, ""));
    }
}
