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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigV2EvaluationTest {

    private static Stream<Arguments> testDateForRuleAndPercentageOptionTest() {
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

    private static Stream<Arguments> testDateForCircularDependencyTest() {
        return Stream.of(
                Arguments.of("key1", "'key1' -> 'key1'"),
                Arguments.of("key2", "'key2' -> 'key3' -> 'key2'"),
                Arguments.of("key4", "'key4' -> 'key3' -> 'key2' -> 'key3'"));
    }

    private static Stream<Arguments> testDateForCircularDependencyMismatchTest() {
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

    @ParameterizedTest
    @MethodSource("testDateForRuleAndPercentageOptionTest")
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
    @MethodSource("testDateForCircularDependencyTest")
    public void prerequisiteFlagCircularDependencyTest(String key, String dependencyCycle) throws IOException {

        ConfigCatClient client = ConfigCatClient.get(Helpers.SDK_KEY, options -> {
            options.flagOverrides(OverrideDataSourceBuilder.classPathResource("test_circulardependency.json"), OverrideBehaviour.LOCAL_ONLY);
        });

        EvaluationDetails<String> result = client.getValueDetails(String.class, key, null, null);
        Assert.assertEquals("java.lang.IllegalArgumentException: Circular dependency detected between the following depending flags: " + dependencyCycle + ".", result.getError());

        ConfigCatClient.closeAll();
    }

    @ParameterizedTest
    @MethodSource("testDateForCircularDependencyMismatchTest")
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
}
