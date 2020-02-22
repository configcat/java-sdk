package com.configcat;

import org.junit.jupiter.api.AfterEach;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class RolloutIntegrationTests {
    private ConfigCatClient client;
    private Scanner csvScanner;

    @Parameterized.Parameters(name
            = "{index}: Test with File={0}, ApiKey={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"testmatrix.csv", "PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A"},
                {"testmatrix_semantic.csv", "PKDVCLf-Hq-h-kCzMp-L7Q/BAr3KgLTP0ObzKnBTo5nhA"},
                {"testmatrix_number.csv", "PKDVCLf-Hq-h-kCzMp-L7Q/uGyK3q9_ckmdxRyI7vjwCw"},
                {"testmatrix_semantic_2.csv", "PKDVCLf-Hq-h-kCzMp-L7Q/q6jMCFIp-EmuAfnmZhPY7w"},
                {"testmatrix_sensitive.csv", "PKDVCLf-Hq-h-kCzMp-L7Q/qX3TP2dTj06ZpCCT1h_SPA"},
        });
    }

    public RolloutIntegrationTests(String fileName, String apiKey) throws FileNotFoundException {
        this.client = ConfigCatClient.newBuilder()
                .build(apiKey);

        ClassLoader classLoader = getClass().getClassLoader();
        this.csvScanner = new Scanner(new File(classLoader.getResource(fileName).getFile()));
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.client.close();
        this.csvScanner.close();
    }

    @Test
    public void testMatrixTest() {

        if(!this.csvScanner.hasNext())
            fail();

        String[] header = this.csvScanner.nextLine().split(";");
        String customKey = header[3];

        String[] settingKeys = Arrays.stream(header).skip(4).toArray(String[]::new);
        ArrayList<String> errors = new ArrayList<>();
        while (this.csvScanner.hasNext()) {
            String[] testObject = this.csvScanner.nextLine().split(";");

            User user = null;
            if(!testObject[0].isEmpty() && !testObject[0].equals("##null##"))
            {
                String email = "";
                String country = "";

                String identifier = testObject[0];

                if(!testObject[1].isEmpty() && !testObject[1].equals("##null##"))
                    email = testObject[1];

                if(!testObject[2].isEmpty() && !testObject[2].equals("##null##"))
                    country = testObject[2];

                Map<String, String> customAttributes = new HashMap<>();
                if(!testObject[3].isEmpty() && !testObject[3].equals("##null##"))
                    customAttributes.put(customKey, testObject[3]);

                user = User.newBuilder()
                        .email(email)
                        .country(country)
                        .custom(customAttributes)
                        .build(identifier);
            }

            int i = 0;
            for (String settingKey: settingKeys) {
                String value = this.client.getValue(String.class, settingKey, user, null);
                if(!value.toLowerCase().equals(testObject[i + 4].toLowerCase())) {
                    errors.add(String.format("Identifier: %s, Key: %s. UV: %s Expected: %s, Result: %s \n", testObject[0], settingKey, testObject[3], testObject[i + 4], value));
                }
                i++;
            }
        }

        assertTrue("Errors found: " + errors.size(), errors.size() == 0);
    }
}
