package com.configcat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RolloutIntegrationTests {
    private static final String APIKEY = "PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A";

    private ConfigCatClient client;
    private Scanner csvScanner;

    @BeforeEach
    public void setUp() throws FileNotFoundException {
        this.client = ConfigCatClient.newBuilder()
                .build(APIKEY);

        ClassLoader classLoader = getClass().getClassLoader();
        this.csvScanner = new Scanner(new File(classLoader.getResource("testmatrix.csv").getFile()));
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

        String[] settingKeys = Arrays.stream(this.csvScanner.nextLine().split(";")).skip(4).toArray(String[]::new);
        StringBuilder errors = new StringBuilder();
        while (this.csvScanner.hasNext()) {
            String[] testObject = this.csvScanner.nextLine().split(";");

            User user = null;
            if(!testObject[0].isEmpty() && !testObject[0].equals("##nouserobject##"))
            {
                Map<String, String> customAttributes = new HashMap<>();
                if(!testObject[3].isEmpty())
                    customAttributes.put("Custom1", testObject[3]);

                user = User.newBuilder()
                        .email(testObject[1])
                        .country(testObject[2])
                        .custom(customAttributes)
                        .build(testObject[0]);
            }

            int i = 0;
            for (String settingKey: settingKeys) {
                String value = this.client.getValue(String.class, settingKey, user, null);
                if(!value.toLowerCase().equals(testObject[i + 4].toLowerCase())) {
                    errors.append(String.format("Identifier: %s, Key: %s. Expected: %s, Result: %s \n", testObject[0], settingKey, testObject[i + 4], value));
                }
                i++;
            }
        }

        assertTrue(errors.length() == 0);
    }
}
