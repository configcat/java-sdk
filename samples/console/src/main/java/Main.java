import com.configcat.ConfigCatClient;

public class Main {
    public static void main(String[] args) {
        ConfigCatClient client = ConfigCatClient
                .newBuilder()
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/PaDVCFk9EpmD6sLpGLltTA");

        // get the configuration serialized to an object
        SampleConfig config = client.getConfiguration(SampleConfig.class, SampleConfig.Empty);

        System.out.println("keyBool: " + config.keyBool);
        System.out.println("keyDouble: " + config.keyDouble);
        System.out.println("keyInteger: " + config.keyInteger);
        System.out.println("keyString: " + config.keyString);

        // get individual config values identified by a key
        System.out.println("keySampleText: " + client.getValue(String.class,"keySampleText", ""));
    }

    static class SampleConfig {
        static SampleConfig Empty = new SampleConfig();

        boolean keyBool;
        double keyDouble;
        int keyInteger;
        String keyString;
    }
}
