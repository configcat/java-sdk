package com.configcat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class EvaluationTest {

    private static final String EVALUATION_FOLDER = "evaluation/";
    private static final String TEST_SDK_KEY = "configcat-sdk-test-key/0000000000000000000000";
    private static final String JSON_EXTENSION = ".json";
    private static final Gson GSON = new Gson();

    private ConfigCatClient client;

    private TestCase[] tests;

    private String testDescriptorName;

    @Parameterized.Parameters(name
            = "{index}: Test with File={0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"simple_value"},
                {"1_targeting_rule"},
                {"2_targeting_rules"},
                {"and_rules"},
                {"semver_validation"},
                {"epoch_date_validation"},
                {"number_validation"},
                {"comparators"},
                {"circular_dependency"},
                {"prerequisite_flag"},
                {"segment"},
                {"options_after_targeting_rule"},
                {"options_based_on_user_id"},
                {"options_based_on_custom_attr"},
                {"options_within_targeting_rule"},
                {"list_truncation"},
        });
    }

    public EvaluationTest(String testDescriptorName) throws IOException {
        this.testDescriptorName = testDescriptorName;
        String testDescriptorContent = readFile(EVALUATION_FOLDER + testDescriptorName + JSON_EXTENSION);
        TestSet testSet = GSON.fromJson(testDescriptorContent, TestSet.class);
        String sdkKey = testSet.getSdkKey();
        if (sdkKey == null || sdkKey.isEmpty()) {
            sdkKey = TEST_SDK_KEY;
        }

        String jsonOverride = testSet.getJsonOverride();

        this.client = ConfigCatClient.get(sdkKey, options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.logLevel(LogLevel.INFO);
            options.baseUrl(testSet.getBaseUrl());
            if (jsonOverride != null && !jsonOverride.isEmpty()) {
                options.flagOverrides(OverrideDataSourceBuilder.classPathResource(EVALUATION_FOLDER + testDescriptorName + "/" + jsonOverride), OverrideBehaviour.LOCAL_OVER_REMOTE);
            }

        });
        //load the data once to run the test
        client.forceRefresh();
        tests = testSet.getTests();

    }

    @AfterEach
    public void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    public void testEvaluation() throws IOException {
        ArrayList<String> errors = new ArrayList<>();
        for (TestCase test : tests) {

            String settingKey = test.getKey();

            Logger clientLogger = (Logger) LoggerFactory.getLogger(ConfigCatClient.class);
            // create and start a ListAppender
            clientLogger.setLevel(Level.INFO);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();

            clientLogger.addAppender(listAppender);

            Class typeOfExpectedResult;
            if (settingKey.startsWith("int") || settingKey.startsWith("whole") || settingKey.startsWith("mainInt")) {
                typeOfExpectedResult = Integer.class;
            } else if (settingKey.startsWith("double") || settingKey.startsWith("decimal") || settingKey.startsWith("mainDouble")) {
                typeOfExpectedResult = Double.class;
            } else if (settingKey.startsWith("boolean") || settingKey.startsWith("bool") || settingKey.startsWith("mainBool") || settingKey.equals("developerAndBetaUserSegment") || settingKey.equals("featureWithSegmentTargeting") || settingKey.equals("featureWithNegatedSegmentTargeting") || settingKey.equals("featureWithNegatedSegmentTargetingCleartext")) {
                typeOfExpectedResult = Boolean.class;
            } else {
                //handle as String in any other case
                typeOfExpectedResult = String.class;
            }

            User user = convertJsonObjectToUser(test.getUser());
            Object result = client.getValue(typeOfExpectedResult, settingKey, user, parseObject(typeOfExpectedResult, test.getDefaultValue()));


            Object returnValue = parseObject(typeOfExpectedResult, test.getReturnValue());
            if (!returnValue.equals(result)) {
                errors.add(String.format("Return value mismatch for test: %s Test Key: %s Expected: %s, Result: %s \n", testDescriptorName, settingKey, returnValue, result));
            }
            String expectedLog = readFile(EVALUATION_FOLDER + testDescriptorName + "/" + test.getExpectedLog());

            StringBuilder logResultBuilder = new StringBuilder();
            List<ILoggingEvent> logsList = listAppender.list;
            logsList.forEach(parseLogEvent(logResultBuilder));
            String logResult = logResultBuilder.toString();
            if (!expectedLog.equals(logResult)) {
                errors.add(String.format("Log mismatch for test: %s Test Key: %s Expected:\n%s\nResult:\n%s\n", testDescriptorName, settingKey, expectedLog, logResult));
            }
        }
        if (errors.size() != 0) {
            System.out.println("\n == ERRORS == \n");
            errors.forEach((error) -> {
                System.out.println(error);
            });
        }
        assertEquals("Errors found: " + errors.size(), 0, errors.size());

    }

    @NotNull
    private static Consumer<ILoggingEvent> parseLogEvent(StringBuilder logResultBuilder) {
        return log ->
                logResultBuilder.append(formatLogLevel(log.getLevel())).append(" ").append(log.getFormattedMessage()).append("\n");
    }

    private static String formatLogLevel(Level level) {
        if (Level.INFO_INT == level.levelInt) {
            return "INFO";
        }
        if (Level.ERROR_INT == level.levelInt) {
            return "ERROR";
        }
        if (Level.WARN_INT == level.levelInt) {
            return "WARNING";
        }
        return "DEBUG";
    }

    private User convertJsonObjectToUser(JsonObject jsonObject) {
        User user = null;
        if (jsonObject != null) {
            String email = "";
            String country = "";
            Set<String> keySet = jsonObject.keySet();
            String identifier = jsonObject.get("Identifier").getAsString();
            keySet.remove("Identifier");
            if (jsonObject.has("Email")) {
                email = jsonObject.get("Email").getAsString();
                keySet.remove("Email");
            }

            if (jsonObject.has("Country")) {
                country = jsonObject.get("Country").getAsString();
                keySet.remove("Country");
            }

            Map<String, String> customAttributes = new HashMap<>();
            keySet.forEach(key -> customAttributes.put(key, jsonObject.get(key).getAsString()));

            user = User.newBuilder()
                    .email(email)
                    .country(country)
                    .custom(customAttributes)
                    .build(identifier);
        }
        return user;
    }

    private Object parseObject(Class<?> classOfT, JsonElement element) {
        if (classOfT == String.class)
            return element.getAsString();
        else if (classOfT == Integer.class || classOfT == int.class)
            return element.getAsInt();
        else if (classOfT == Double.class || classOfT == double.class)
            return element.getAsDouble();
        else if (classOfT == Boolean.class || classOfT == boolean.class)
            return element.getAsBoolean();
        else
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");
    }


    private String readFile(String filePath) throws IOException {

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (stream == null) {
                throw new IOException();
            }
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int temp;
            while ((temp = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, temp);
            }
            return new String(outputStream.toByteArray(), Charset.defaultCharset());
        }
    }
}

class TestSet {
    @SerializedName(value = "sdkKey")
    private String sdkKey;
    @SerializedName(value = "baseUrl")
    private String baseUrl;
    @SerializedName(value = "jsonOverride")
    private String jsonOverride;
    @SerializedName(value = "tests")
    private TestCase[] tests;

    public String getSdkKey() {
        return sdkKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getJsonOverride() {
        return jsonOverride;
    }

    public TestCase[] getTests() {
        return tests;
    }
}

class TestCase {
    @SerializedName(value = "key")
    private String key;
    @SerializedName(value = "defaultValue")
    private JsonElement defaultValue;
    @SerializedName(value = "returnValue")
    private JsonElement returnValue;
    @SerializedName(value = "expectedLog")
    private String expectedLog;
    @SerializedName(value = "user")
    private JsonObject user;

    public String getKey() {
        return key;
    }

    public JsonElement getDefaultValue() {
        return defaultValue;
    }

    public JsonElement getReturnValue() {
        return returnValue;
    }

    public String getExpectedLog() {
        return expectedLog;
    }

    public JsonObject getUser() {
        return user;
    }
}
