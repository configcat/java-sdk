package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EvaluatorTrimTest {
    private final static String TEST_IDENTIFIER = "12345";
    private final static String TEST_VERSION = "1.0.0";
    private final static String TEST_NUMBER = "3";

    private final static String TEST_COUNTRY = "[\"USA\"]";

    private final static String TEST_COUNTRY_WITH_WHITESPACES = "[\" USA \"]";

    //1705253400 - 2014.01.14 17:30:00 +1 - test check between 17:00 and 18:00
    private final static String TEST_DATE = "1705253400";
    /**
     * trim_user_values.json contains valid settings. Expect "containsanyof" and "notcontainsanyof" flags where the
     * comparator values contains pre and post white spaces, the untrimmed user value can be properly compared
     * against the invalid data.
     */
    private final static String TRIM_USER_VALUES_JSON = "trim_user_values.json";
    /**
     * trim_comparator_values.json contains settings with invalid comparator values. The server default handles the
     * trimming. To test the client the comparator values contains pre and post whitespaces.
     */
    private final static String TRIM_COMPARATOR_VALUES_JSON = "trim_comparator_values.json";

    private ConfigCatClient client;
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.client = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.httpClient(new OkHttpClient.Builder().build());
            options.pollingMode(PollingModes.lazyLoad(2));
            options.baseUrl(this.server.url("/").toString());
        });
    }

    @AfterEach
    void tearDown() throws IOException {
        ConfigCatClient.closeAll();
        this.server.shutdown();
    }

    private String loadJsonFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());
        byte[] byteArray = Files.readAllBytes(file.toPath());
        return new String(byteArray);
    }

    private User createTestUser(String identifier, String country, String version, String number, String date) {
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("Version", version);
        customAttributes.put("Number", number);
        customAttributes.put("Date", date);
        return User.newBuilder().country(country).custom(customAttributes).build(identifier);
    }

    private String addWhiteSpaces(String raw) {
        return " " + raw + " ";
    }

    private static Stream<Arguments> testComparatorValueTrimsData() {
        return Stream.of(
                Arguments.of("isoneof", "no trim"),
                Arguments.of("isnotoneof", "no trim"),
                Arguments.of("containsanyof", "no trim"),
                Arguments.of("notcontainsanyof", "no trim"),
                Arguments.of("isoneofhashed", "no trim"),
                Arguments.of("isnotoneofhashed", "no trim"),
                Arguments.of("equalshashed", "no trim"),
                Arguments.of("notequalshashed", "no trim"),
                Arguments.of("arraycontainsanyofhashed", "no trim"),
                Arguments.of("arraynotcontainsanyofhashed", "no trim"),
                Arguments.of("equals", "no trim"),
                Arguments.of("notequals", "no trim"),
                Arguments.of("startwithanyof", "no trim"),
                Arguments.of("notstartwithanyof", "no trim"),
                Arguments.of("endswithanyof", "no trim"),
                Arguments.of("notendswithanyof", "no trim"),
                Arguments.of("arraycontainsanyof", "no trim"),
                Arguments.of("arraynotcontainsanyof", "no trim"),
                // the not trimmed comparator value case an exception in case of these comparator, default value expected
                Arguments.of("startwithanyofhashed", "default"),
                Arguments.of("notstartwithanyofhashed", "default"),
                Arguments.of("endswithanyofhashed", "default"),
                Arguments.of("notendswithanyofhashed", "default"),
                //semver comparator values trimmed because of backward compatibility
                Arguments.of("semverisoneof", "4 trim"),
                Arguments.of("semverisnotoneof", "5 trim"),
                Arguments.of("semverless", "6 trim"),
                Arguments.of("semverlessequals", "7 trim"),
                Arguments.of("semvergreater", "8 trim"),
                Arguments.of("semvergreaterequals", "9 trim")
        );
    }

    @ParameterizedTest
    @MethodSource("testComparatorValueTrimsData")
    void testComparatorValueTrims(String key, String expectedValue) throws IOException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(loadJsonFileAsString(TRIM_COMPARATOR_VALUES_JSON)));
        User user = createTestUser(TEST_IDENTIFIER, TEST_COUNTRY, TEST_VERSION, TEST_NUMBER, TEST_DATE);
        String result = this.client.getValue(String.class, key, user, "default");
        assertEquals(expectedValue, result);
    }

    private static Stream<Arguments> testUserValueTrimsData() {
        return Stream.of(
                Arguments.of("isoneof", "no trim"),
                Arguments.of("isnotoneof", "no trim"),
                Arguments.of("isoneofhashed", "no trim"),
                Arguments.of("isnotoneofhashed", "no trim"),
                Arguments.of("equalshashed", "no trim"),
                Arguments.of("notequalshashed", "no trim"),
                Arguments.of("arraycontainsanyofhashed", "no trim"),
                Arguments.of("arraynotcontainsanyofhashed", "no trim"),
                Arguments.of("equals", "no trim"),
                Arguments.of("notequals", "no trim"),
                Arguments.of("startwithanyof", "no trim"),
                Arguments.of("notstartwithanyof", "no trim"),
                Arguments.of("endswithanyof", "no trim"),
                Arguments.of("notendswithanyof", "no trim"),
                Arguments.of("arraycontainsanyof", "no trim"),
                Arguments.of("arraynotcontainsanyof", "no trim"),
                Arguments.of("startwithanyofhashed", "no trim"),
                Arguments.of("notstartwithanyofhashed", "no trim"),
                Arguments.of("endswithanyofhashed", "no trim"),
                Arguments.of("notendswithanyofhashed", "no trim"),
                //semver comparators user values trimmed because of backward compatibility
                Arguments.of("semverisoneof", "4 trim"),
                Arguments.of("semverisnotoneof", "5 trim"),
                Arguments.of("semverless", "6 trim"),
                Arguments.of("semverlessequals", "7 trim"),
                Arguments.of("semvergreater", "8 trim"),
                Arguments.of("semvergreaterequals", "9 trim"),
                //number and date comparators user values trimmed because of backward compatibility
                Arguments.of("numberequals", "10 trim"),
                Arguments.of("numbernotequals", "11 trim"),
                Arguments.of("numberless", "12 trim"),
                Arguments.of("numberlessequals", "13 trim"),
                Arguments.of("numbergreater", "14 trim"),
                Arguments.of("numbergreaterequals", "15 trim"),
                Arguments.of("datebefore", "18 trim"),
                Arguments.of("dateafter", "19 trim"),
                //"contains any of" and "not contains any of" is a special case, the not trimmed user attribute checked against not trimmed comparator values.
                Arguments.of("containsanyof", "no trim"),
                Arguments.of("notcontainsanyof", "no trim")
        );
    }

    @ParameterizedTest
    @MethodSource("testUserValueTrimsData")
    void testUserValueTrims(String key, String expectedValue) throws IOException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(loadJsonFileAsString(TRIM_USER_VALUES_JSON)));
        User user = createTestUser(addWhiteSpaces(TEST_IDENTIFIER), TEST_COUNTRY_WITH_WHITESPACES, addWhiteSpaces(TEST_VERSION), addWhiteSpaces(TEST_NUMBER), addWhiteSpaces(TEST_DATE));
        String result = this.client.getValue(String.class, key, user, "default");
        assertEquals(expectedValue, result);
    }
}
