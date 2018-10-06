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

        // get the configuration serialized to an object
        SampleConfig config = client.getConfiguration(SampleConfig.class, user, SampleConfig.Empty);

        System.out.println("bool30TrueAdvancedRules: " + config.bool30TrueAdvancedRules);
        System.out.println("double25Pi25E25Gr25Zero: " + config.double25Pi25E25Gr25Zero);
        System.out.println("integer25One25Two25Three25FourAdvancedRules: " + config.integer25One25Two25Three25FourAdvancedRules);
        System.out.println("string25Cat25Dog25Falcon25Horse: " + config.string25Cat25Dog25Falcon25Horse);

        // get individual config values identified by a key for a user
        System.out.println("keySampleText: " + client.getValue(String.class,"keySampleText", user, ""));
    }

    static class SampleConfig {
        static SampleConfig Empty = new SampleConfig();

        Boolean bool30TrueAdvancedRules;
        Double double25Pi25E25Gr25Zero;
        Integer integer25One25Two25Three25FourAdvancedRules;
        String string25Cat25Dog25Falcon25Horse;
    }
}
