package com.configcat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Assert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class ConfigV2EvaluationTest {

    private static Stream<Arguments> testDataForRuleAndPercentageOptionTest() {
        return Stream.of(
                Arguments.of(null, null, null, "Cat", false, false),
                Arguments.of("12345", null, null, "Cat", false, false),
                Arguments.of("12345", "a@example.com", null, "Dog", true, false),
                Arguments.of("12345", "a@configcat.com", null, "Cat", false, false),
                Arguments.of("12345", "a@configcat.com", "", "Frog", true, true),
                Arguments.of("12345", "a@configcat.com", "US", "Fish", true, true),
                Arguments.of("12345", "b@configcat.com", null, "Cat", false, false),
                Arguments.of("12345", "b@configcat.com", "", "Falcon", false, true),
                Arguments.of("12345", "b@configcat.com", "US", "Spider", false, true));
    }

    private static Stream<Arguments> testDataForCircularDependencyTest() {
        return Stream.of(
                Arguments.of("key1", "'key1' -> 'key1'"),
                Arguments.of("key2", "'key2' -> 'key3' -> 'key2'"),
                Arguments.of("key4", "'key4' -> 'key3' -> 'key2' -> 'key3'"));
    }

    private static Stream<Arguments> testDataForPrerequisiteFlagTypeMismatchTest() {
        return Stream.of(
                Arguments.of("stringDependsOnBool", "mainBoolFlag", true, "Dog"),
                Arguments.of("stringDependsOnBool", "mainBoolFlag", false, "Cat"),
                Arguments.of("stringDependsOnBool", "mainBoolFlag", "1", null),
                Arguments.of("stringDependsOnBool", "mainBoolFlag", 1, null),
                Arguments.of("stringDependsOnBool", "mainBoolFlag", 1.0, null),
                Arguments.of("stringDependsOnString", "mainStringFlag", "private", "Dog"),
                Arguments.of("stringDependsOnString", "mainStringFlag", "Private", "Cat"),
                Arguments.of("stringDependsOnString", "mainStringFlag", true, null),
                Arguments.of("stringDependsOnString", "mainStringFlag", 1, null),
                Arguments.of("stringDependsOnString", "mainStringFlag", 1.0, null),
                Arguments.of("stringDependsOnInt", "mainIntFlag", 2, "Dog"),
                Arguments.of("stringDependsOnInt", "mainIntFlag", 1, "Cat"),
                Arguments.of("stringDependsOnInt", "mainIntFlag", "2", null),
                Arguments.of("stringDependsOnInt", "mainIntFlag", true, null),
                Arguments.of("stringDependsOnInt", "mainIntFlag", 2.0, null),
                Arguments.of("stringDependsOnDouble", "mainDoubleFlag", 0.1, "Dog"),
                Arguments.of("stringDependsOnDouble", "mainDoubleFlag", 0.11, "Cat"),
                Arguments.of("stringDependsOnDouble", "mainDoubleFlag", "0.1", null),
                Arguments.of("stringDependsOnDouble", "mainDoubleFlag", true, null),
                Arguments.of("stringDependsOnDouble", "mainDoubleFlag", 1, null)
        );
    }

    private static Stream<Arguments> testDataForPrerequisiteFlagOverrideTest() {
        return Stream.of(
                Arguments.of("stringDependsOnString", "1", "john@sensitivecompany.com", null, "Dog"),
                Arguments.of("stringDependsOnString", "1", "john@sensitivecompany.com", OverrideBehaviour.REMOTE_OVER_LOCAL, "Dog"),
                Arguments.of("stringDependsOnString", "1", "john@sensitivecompany.com", OverrideBehaviour.LOCAL_OVER_REMOTE, "Dog"),
                Arguments.of("stringDependsOnString", "1", "john@sensitivecompany.com", OverrideBehaviour.LOCAL_ONLY, null),
                Arguments.of("stringDependsOnString", "2", "john@notsensitivecompany.com", null, "Cat"),
                Arguments.of("stringDependsOnString", "2", "john@notsensitivecompany.com", OverrideBehaviour.REMOTE_OVER_LOCAL, "Cat"),
                Arguments.of("stringDependsOnString", "2", "john@notsensitivecompany.com", OverrideBehaviour.LOCAL_OVER_REMOTE, "Dog"),
                Arguments.of("stringDependsOnString", "2", "john@notsensitivecompany.com", OverrideBehaviour.LOCAL_ONLY, null),
                Arguments.of("stringDependsOnInt", "1", "john@sensitivecompany.com", null, "Dog"),
                Arguments.of("stringDependsOnInt", "1", "john@sensitivecompany.com", OverrideBehaviour.REMOTE_OVER_LOCAL, "Dog"),
                Arguments.of("stringDependsOnInt", "1", "john@sensitivecompany.com", OverrideBehaviour.LOCAL_OVER_REMOTE, "Cat"),
                Arguments.of("stringDependsOnInt", "1", "john@sensitivecompany.com", OverrideBehaviour.LOCAL_ONLY, null),
                Arguments.of("stringDependsOnInt", "2", "john@notsensitivecompany.com", null, "Cat"),
                Arguments.of("stringDependsOnInt", "2", "john@notsensitivecompany.com", OverrideBehaviour.REMOTE_OVER_LOCAL, "Cat"),
                Arguments.of("stringDependsOnInt", "2", "john@notsensitivecompany.com", OverrideBehaviour.LOCAL_OVER_REMOTE, "Dog"),
                Arguments.of("stringDependsOnInt", "2", "john@notsensitivecompany.com", OverrideBehaviour.LOCAL_ONLY, null)
        );
    }

    private static Stream<Arguments> testDataForConfigSaltAndSegmentsOverrideTest() {
        return Stream.of(
                Arguments.of("developerAndBetaUserSegment", "1", "john@example.com", null, false),
                Arguments.of("developerAndBetaUserSegment", "1", "john@example.com", OverrideBehaviour.REMOTE_OVER_LOCAL, false),
                Arguments.of("developerAndBetaUserSegment", "1", "john@example.com", OverrideBehaviour.LOCAL_OVER_REMOTE, true),
                Arguments.of("developerAndBetaUserSegment", "1", "john@example.com", OverrideBehaviour.LOCAL_ONLY, true),
                Arguments.of("notDeveloperAndNotBetaUserSegment", "2", "kate@example.com", null, true),
                Arguments.of("notDeveloperAndNotBetaUserSegment", "2", "kate@example.com", OverrideBehaviour.REMOTE_OVER_LOCAL, true),
                Arguments.of("notDeveloperAndNotBetaUserSegment", "2", "kate@example.com", OverrideBehaviour.LOCAL_OVER_REMOTE, true),
                Arguments.of("notDeveloperAndNotBetaUserSegment", "2", "kate@example.com", OverrideBehaviour.LOCAL_ONLY, null)
        );
    }

    private static Stream<Arguments> testDataComparisonAttributeConversionToCanonicalStringRepresentationTest() {
        return Stream.of(
                Arguments.of("numberToStringConversion", .12345, "1"),
                Arguments.of("numberToStringConversion", 0.12345, "1"),
                Arguments.of("numberToStringConversion", .12345d, "1"),
                Arguments.of("numberToStringConversion", 0.12345d, "1"),
                Arguments.of("numberToStringConversion", .12345f, "1"),
                Arguments.of("numberToStringConversion", 0.12345f, "1"),
                Arguments.of("numberToStringConversionInt", (byte)125, "4"),
                Arguments.of("numberToStringConversionInt", (short)125, "4"),
                Arguments.of("numberToStringConversionInt", 125, "4"),
                Arguments.of("numberToStringConversionInt", 125L, "4"),
                Arguments.of("numberToStringConversionPositiveExp", -1.23456789e96, "2"),
                Arguments.of("numberToStringConversionNegativeExp", -12345.6789E-100, "4"),
                Arguments.of("numberToStringConversionNaN", Double.NaN, "3"),
                Arguments.of("numberToStringConversionPositiveInf", Double.POSITIVE_INFINITY, "4"),
                Arguments.of("numberToStringConversionNegativeInf", Double.NEGATIVE_INFINITY, "3"),
                Arguments.of("numberToStringConversionPositiveExp", -1.23456789e96d, "2"),
                Arguments.of("numberToStringConversionNegativeExp", -12345.6789E-100d, "4"),
                Arguments.of("numberToStringConversionNaN", Float.NaN, "3"),
                Arguments.of("numberToStringConversionPositiveInf", Float.POSITIVE_INFINITY, "4"),
                Arguments.of("numberToStringConversionNegativeInf", Float.NEGATIVE_INFINITY, "3"),
                Arguments.of("dateToStringConversion",  "date:2023-03-31T23:59:59.9990000Z", "3"),
                Arguments.of("dateToStringConversion",  "instant:2023-03-31T23:59:59.9990000Z", "3"),
                Arguments.of("dateToStringConversion", 1680307199.999, "3"),
                Arguments.of("dateToStringConversionNaN", Double.NaN, "3"),
                Arguments.of("dateToStringConversionPositiveInf", Double.POSITIVE_INFINITY, "1"),
                Arguments.of("dateToStringConversionNegativeInf", Double.NEGATIVE_INFINITY, "5"),
                Arguments.of("stringArrayToStringConversion", new String[] { "read", "Write", " eXecute " }, "4"),
                Arguments.of("stringArrayToStringConversionEmpty", new String[0], "5"),
                Arguments.of("stringArrayToStringConversionSpecialChars", new String[] { "+<>%\"'\\/\t\r\n" }, "3"),
                Arguments.of("stringArrayToStringConversionUnicode", "specialCharacter:specialCharacters.txt", "2")
        );
    }

    @ParameterizedTest
    @MethodSource("testDataForRuleAndPercentageOptionTest")
    public void matchedEvaluationRuleAndPercentageOption(String userId, String email, String percentageBaseCustom, String expectedValue, boolean expectedTargetingRule, boolean expectedPercentageOption) throws IOException {

        ConfigCatClient client = ConfigCatClient.get("configcat-sdk-1/JcPbCGl_1E-K9M-fJOyKyQ/P4e3fAz_1ky2-Zg2e4cbkw");

        User user = null;
        if (userId != null) {

            Map<String, Object> customAttributes = new HashMap<>();
            if (percentageBaseCustom != null) {
                customAttributes.put("PercentageBase", percentageBaseCustom);
            }

            user = User.newBuilder()
                    .email(email)
                    .custom(customAttributes)
                    .build(userId);
        }

        EvaluationDetails<String> result = client.getValueDetails(String.class, "stringMatchedTargetingRuleAndOrPercentageOption", user, null);

        Assert.assertEquals(expectedValue, result.getValue());
        Assert.assertEquals(expectedTargetingRule, result.getMatchedTargetingRule() != null);
        Assert.assertEquals(expectedPercentageOption, result.getMatchedPercentageOption() != null);

        ConfigCatClient.closeAll();
    }

    @ParameterizedTest
    @MethodSource("testDataForCircularDependencyTest")
    public void prerequisiteFlagCircularDependencyTest(String key, String dependencyCycle) throws IOException {

        ConfigCatClient client = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.flagOverrides(OverrideDataSourceBuilder.classPathResource("test_circulardependency.json"), OverrideBehaviour.LOCAL_ONLY);
        });

        EvaluationDetails<String> result = client.getValueDetails(String.class, key, null, null);
        Assert.assertEquals("java.lang.IllegalArgumentException: Circular dependency detected between the following depending flags: " + dependencyCycle + ".", result.getError());

        ConfigCatClient.closeAll();
    }

    @ParameterizedTest
    @MethodSource("testDataForPrerequisiteFlagTypeMismatchTest")
    public void prerequisiteFlagTypeMismatchTest(String key, String prerequisiteFlagKey, Object prerequisiteFlagValue, String expectedValue) throws IOException {

        Logger clientLogger = (Logger) LoggerFactory.getLogger(ConfigCatClient.class);
        // create and start a ListAppender
        clientLogger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        clientLogger.addAppender(listAppender);

        Map<String, Object> map = new HashMap<>();
        map.put(prerequisiteFlagKey, prerequisiteFlagValue);

        //rollout test - prerequisite flag sdkKey
        ConfigCatClient client = ConfigCatClient.get("configcat-sdk-1/JcPbCGl_1E-K9M-fJOyKyQ/JoGwdqJZQ0K2xDy7LnbyOg", options -> {
            options.flagOverrides(OverrideDataSourceBuilder.map(map), OverrideBehaviour.LOCAL_OVER_REMOTE);
            options.pollingMode(PollingModes.manualPoll());
        });

        client.forceRefresh();

        String value = client.getValue(String.class, key, null, null);

        Assert.assertEquals(expectedValue, value);

        if (expectedValue == null) {
            List<ILoggingEvent> logsList = listAppender.list;

            List<ILoggingEvent> errorLogs = logsList.stream().filter(iLoggingEvent -> iLoggingEvent.getLevel().equals(Level.ERROR)).collect(Collectors.toList());
            Assert.assertEquals(1, errorLogs.size());
            String errorMessage = errorLogs.get(0).getFormattedMessage();
            String causeExceptionMessage = errorLogs.get(0).getThrowableProxy().getMessage();

            Assert.assertTrue(errorMessage.contains("[2001]"));

            if (prerequisiteFlagValue == null) {
                Assert.assertTrue(causeExceptionMessage.contains("Setting value is null"));
            } else {
                Assert.assertTrue(causeExceptionMessage.contains("Type mismatch between comparison value"));
            }

        }

        ConfigCatClient.closeAll();
    }

    @ParameterizedTest
    @MethodSource("testDataForPrerequisiteFlagOverrideTest")
    public void prerequisiteFlagOverrideTest(String key, String userId, String email, OverrideBehaviour overrideBehaviour, Object expectedValue) throws IOException {
        User user = null;
        if (userId != null) {
            user = User.newBuilder()
                    .email(email)
                    .build(userId);
        }

        ConfigCatClient client = ConfigCatClient.get("configcat-sdk-1/JcPbCGl_1E-K9M-fJOyKyQ/JoGwdqJZQ0K2xDy7LnbyOg", options -> {
            if (overrideBehaviour != null) {
                options.flagOverrides(OverrideDataSourceBuilder.classPathResource("test_override_flagdependency_v6.json"), overrideBehaviour);
            }
            options.pollingMode(PollingModes.manualPoll());
        });

        client.forceRefresh();

        String value = client.getValue(String.class, key, user, null);

        Assert.assertEquals(expectedValue, value);

        ConfigCatClient.closeAll();
    }

    @ParameterizedTest
    @MethodSource("testDataForConfigSaltAndSegmentsOverrideTest")
    public void configSaltAndSegmentsOverrideTest(String key, String userId, String email, OverrideBehaviour overrideBehaviour, Object expectedValue) throws IOException {
        User user = null;
        if (userId != null) {
            user = User.newBuilder()
                    .email(email)
                    .build(userId);
        }

        ConfigCatClient client = ConfigCatClient.get("configcat-sdk-1/JcPbCGl_1E-K9M-fJOyKyQ/h99HYXWWNE2bH8eWyLAVMA", options -> {
            if (overrideBehaviour != null) {
                options.flagOverrides(OverrideDataSourceBuilder.classPathResource("test_override_segments_v6.json"), overrideBehaviour);
            }
            options.pollingMode(PollingModes.manualPoll());
        });

        client.forceRefresh();

        Boolean value = client.getValue(Boolean.class, key, user, null);

        Assert.assertEquals(expectedValue, value);

        ConfigCatClient.closeAll();
    }

    @ParameterizedTest
    @MethodSource("testDataComparisonAttributeConversionToCanonicalStringRepresentationTest")
    public void comparisonAttributeConversionToCanonicalStringRepresentationTest(String key, Object userAttribute, String expectedValue) throws IOException, ParseException {
        ConfigCatClient client = ConfigCatClient.get("configcat-sdk-1/TEST_KEY-0123456789012/1234567890123456789012", options -> {
            options.flagOverrides(OverrideDataSourceBuilder.classPathResource("comparison_attribute_conversion.json"), OverrideBehaviour.LOCAL_ONLY);
            options.pollingMode(PollingModes.manualPoll());
        });
        if(userAttribute instanceof String){
            String userAttributeString = (String) userAttribute;
            if (userAttributeString.startsWith("date:")){
                Instant instant = Instant.parse(userAttributeString.substring(5));
                userAttribute = Date.from(instant);
            }
            if (userAttributeString.startsWith("instant:")){
                userAttribute = Instant.parse(userAttributeString.substring(8));
            }
            if (userAttributeString.startsWith("specialCharacter:")){
                ClassLoader classLoader = getClass().getClassLoader();

                Scanner scanner = new Scanner(new File(Objects.requireNonNull(classLoader.getResource(userAttributeString.substring(17))).getFile()), "UTF-8");
                if(!scanner.hasNext()){
                    fail();
                }
                String specialCharacters = scanner.nextLine();
                userAttribute =  new String[] {specialCharacters};
            }

        }
        Map<String, Object> custom = new HashMap<>();
        custom.put("Custom1", userAttribute);

        User user = User.newBuilder()
                    .custom(custom)
                    .build("12345");

        String value = client.getValue(String.class, key, user, "default");

        Assert.assertEquals(expectedValue, value);

        ConfigCatClient.closeAll();
    }
}
