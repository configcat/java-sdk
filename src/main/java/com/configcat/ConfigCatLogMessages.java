package com.configcat;

import java.util.Set;
import java.util.stream.Collectors;

final class ConfigCatLogMessages {

    /**
     * Log message for Config Service Cannot Initiate Http Calls warning. The log eventId 3200.
     */
    public static final String CONFIG_SERVICE_CANNOT_INITIATE_HTTP_CALLS_WARN = "Client is in offline mode, it cannot initiate HTTP calls.";

    /**
     * Log message for Data Governance Is Out Of Sync warning. The log eventId 3002.
     */
    public static final String DATA_GOVERNANCE_IS_OUT_OF_SYNC_WARN = "The `builder.dataGovernance()` parameter specified at the client initialization is not in sync with the preferences on the ConfigCat Dashboard. Read more: https://configcat.com/docs/advanced/data-governance/";
    /**
     * Log message for Config Service Cache Write error. The log eventId is 2201.
     */
    public static final String CONFIG_SERVICE_CACHE_WRITE_ERROR = "Error occurred while writing the cache";
    /**
     * Log message for Config Service Cache Read error. The log eventId is 2200.
     */
    public static final String CONFIG_SERVICE_CACHE_READ_ERROR = "Error occurred while reading the cache.";
    /**
     * Log message for Fetch Received 200 With Invalid Body error. The log eventId is 1105.
     */
    public static final String FETCH_RECEIVED_200_WITH_INVALID_BODY_ERROR = "Fetching config JSON was successful but the HTTP response content was invalid.";
    /**
     * Log message for Fetch Failed Due To Redirect Loop error. The log eventId is 1104.
     */
    public static final String FETCH_FAILED_DUE_TO_REDIRECT_LOOP_ERROR = "Redirection loop encountered while trying to fetch config JSON. Please contact us at https://configcat.com/support/";

    /**
     * Log message for Fetch Failed Due To Unexpected error. The log eventId is 1103.
     */
    public static final String FETCH_FAILED_DUE_TO_UNEXPECTED_ERROR = "Unexpected error occurred while trying to fetch config JSON. It is most likely due to a local network issue. Please make sure your application can reach the ConfigCat CDN servers (or your proxy server) over HTTP.";

    /**
     * Log message for Fetch Failed Due To Invalid Sdk Key error. The log eventId is 1100.
     */
    public static final String FETCH_FAILED_DUE_TO_INVALID_SDK_KEY_ERROR = "Your SDK Key seems to be wrong. You can find the valid SDK Key at https://app.configcat.com/sdkkey";

    private ConfigCatLogMessages() { /* prevent from instantiation*/ }

    /**
     * Log message for Config Json Is Not Presented errors when the method returns with default value. The log eventId is 1000.
     *
     * @param key               The feature flag key.
     * @param defaultParamName  The default parameter name.
     * @param defaultParamValue The default parameter value.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getConfigJsonIsNotPresentedWithDefaultValue(final String key, final String defaultParamName, final Object defaultParamValue) {
        return new FormattableLogMessage("Config JSON is not present when evaluating setting '%s'. Returning the `%s` parameter that you specified in your application: '%s'.", key, defaultParamName, defaultParamValue);
    }

    /**
     * Log message for Config Json Is Not Presented errors when the method returns with empty value. The log eventId is 1000.
     *
     * @param emptyResult The empty result.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getConfigJsonIsNotPresentedWithEmptyResult(final String emptyResult) {
        return new FormattableLogMessage("Config JSON is not present. Returning %s.", emptyResult);
    }

    /**
     * Log message for Setting Evaluation Failed Due To Missing Key error. The log eventId is 1001.
     *
     * @param key               The feature flag key.
     * @param defaultParamName  The default parameter name.
     * @param defaultParamValue The default parameter value.
     * @param availableKeysSet  The set of available keys in the settings.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getSettingEvaluationFailedDueToMissingKey(final String key, final String defaultParamName, final Object defaultParamValue, final Set<String> availableKeysSet) {
        return new FormattableLogMessageWithKeySet("Failed to evaluate setting '%s' (the key was not found in config JSON). Returning the `%s` parameter that you specified in your application: '%s'. Available keys: [%s].", key, defaultParamName, defaultParamValue, availableKeysSet);
    }

    /**
     * Log message for Setting Evaluation errors when the method returns with default value. The log eventId is 1002.
     *
     * @param methodName        The method name where the error is logged.
     * @param key               The feature flag key.
     * @param defaultParamName  The default parameter name.
     * @param defaultParamValue The default parameter value.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getSettingEvaluationErrorWithDefaultValue(final String methodName, final String key, final String defaultParamName, final Object defaultParamValue) {
        return new FormattableLogMessage("Error occurred in the `%s` method while evaluating setting '%s'. Returning the `%s` parameter that you specified in your application: '%s'.", methodName, key, defaultParamName, defaultParamValue);
    }

    /**
     * Log message for Setting Evaluation errors when the method returns with empty value. The log eventId is 1002.
     *
     * @param methodName  The method name where the error is logged.
     * @param emptyResult The empty result.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getSettingEvaluationErrorWithEmptyValue(final String methodName, final String emptyResult) {
        return new FormattableLogMessage("Error occurred in the `%s` method. Returning %s.", methodName, emptyResult);
    }

    /**
     * Log message for Force Refresh errors. The log eventId is 1003.
     *
     * @param methodName The method name where the error is logged.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getForceRefreshError(final String methodName) {
        return new FormattableLogMessage("Error occurred in the `%s` method.", methodName);
    }

    /**
     * Log message for Setting Evaluation Failed For Other Reason errors. The log eventId is 2001.
     *
     * @param key               The feature flag key.
     * @param defaultParamName  The default parameter name.
     * @param defaultParamValue The default parameter value.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getSettingEvaluationFailedForOtherReason(final String key, final String defaultParamName, final Object defaultParamValue) {
        return new FormattableLogMessage("Failed to evaluate setting '%s'. Returning the `%s` parameter that you specified in your application: '%s'.", key, defaultParamName, defaultParamValue);
    }

    /**
     * Log message for Setting For Variation Id Is Not Present error. The log eventId is 2011.
     *
     * @param variationId The variation id.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getSettingForVariationIdIsNotPresent(final String variationId) {
        return new FormattableLogMessage("Could not find the setting for the specified variation ID: '%s'.", variationId);
    }

    /**
     * Log message for Fetch Failed Due To Unexpected Http Response error. The log eventId is 1101.
     *
     * @param responseCode    The http response code.
     * @param responseMessage The http response message.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getFetchFailedDueToUnexpectedHttpResponse(final int responseCode, final String responseMessage) {
        return new FormattableLogMessage("Unexpected HTTP response was received while trying to fetch config JSON: %d %s", responseCode, responseMessage);
    }

    /**
     * Log message for Fetch Failed Due To Request Timeout error. The log eventId is 1102.
     *
     * @param connectTimeoutMillis Connect timeout in milliseconds.
     * @param readTimeoutMillis    Read timeout in milliseconds.
     * @param writeTimeoutMillis   Write timeout in milliseconds.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getFetchFailedDueToRequestTimeout(final Integer connectTimeoutMillis, final Integer readTimeoutMillis, final Integer writeTimeoutMillis) {
        return new FormattableLogMessage("Request timed out while trying to fetch config JSON. Timeout values: [connect: %dms, read: %dms, write: %dms]", connectTimeoutMillis, readTimeoutMillis, writeTimeoutMillis);
    }

    /**
     * Log message for Local File Data Source Does Not Exist error. The log eventId is 1300.
     *
     * @param filePath The file path.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getLocalFileDataSourceDoesNotExist(final String filePath) {
        return new FormattableLogMessage("Cannot find the local config file '%s'. This is a path that your application provided to the ConfigCat SDK by passing it to the `OverrideDataSourceBuilder.localFile()` method. Read more: https://configcat.com/docs/sdk-reference/java/#json-file", filePath);
    }

    /**
     * Log message for Local File Data Source Failed To Read File error. The log eventId is 1302.
     *
     * @param filePath The file path.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getLocalFileDataSourceFailedToReadFile(final String filePath) {
        return new FormattableLogMessage("Failed to read the local config file '%s'.", filePath);
    }

    /**
     * Log message for Client Is Already Created warning. The log eventId 3000.
     *
     * @param sdkKey The ConfigCat client SDK key.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getClientIsAlreadyCreated(final String sdkKey) {
        return new FormattableLogMessage("There is an existing client instance for the specified SDK Key. No new client instance will be created and the specified options callback is ignored. Returning the existing client instance. SDK Key: '%s'.", sdkKey);
    }

    /**
     * Log message for User Object is missing warning. The log eventId 3001.
     *
     * @param key The feature flag setting key.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getUserObjectMissing(final String key) {
        return new FormattableLogMessage("Cannot evaluate targeting rules and %% options for setting '%s' (User Object is missing). You should pass a User Object to the evaluation methods like `getValue()`/`getValueAsync()` in order to make targeting work properly. Read more: https://configcat.com/docs/advanced/user-object/", key);
    }

    /**
     * Log message for User Attribute is missing warning. The log eventId 3003.
     *
     * @param key           The feature flag setting key.
     * @param userCondition The user condition where the attribute is checked.
     * @param attributeName The user attribute name.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getUserAttributeMissing(final String key, final UserCondition userCondition, final String attributeName) {
        return new FormattableLogMessageWithUserCondition("Cannot evaluate condition (%s) for setting '%s' (the User.%s attribute is missing). You should set the User.%s attribute in order to make targeting work properly. Read more: https://configcat.com/docs/advanced/user-object/", userCondition, key, attributeName, attributeName);
    }

    /**
     * Log message for User Attribute is missing warning. The log eventId 3003.
     *
     * @param key           The feature flag setting key.
     * @param attributeName The user attribute name.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getUserAttributeMissing(final String key, final String attributeName) {
        return new FormattableLogMessage("Cannot evaluate %% options for setting '%s' (the User.%s attribute is missing). You should set the User.%s attribute in order to make targeting work properly. Read more: https://configcat.com/docs/advanced/user-object/", key, attributeName, attributeName);
    }

    /**
     * Log message for User Attribute is invalid warning. The log eventId 3004.
     *
     * @param key           The feature flag setting key.
     * @param userCondition The user condition where the attribute is checked.
     * @param reason        Why the attribute is invalid.
     * @param attributeName The user attribute name.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getUserAttributeInvalid(final String key, final UserCondition userCondition, final String reason, final String attributeName) {
        return new FormattableLogMessageWithUserCondition("Cannot evaluate condition (%s) for setting '%s' (%s). Please check the User.%s attribute and make sure that its value corresponds to the comparison operator.", userCondition, key, reason, attributeName);
    }


    /**
     * Log message for User Attribute value is automatically converted warning. The log eventId 3005.
     *
     * @param key            The feature flag setting key.
     * @param userCondition  The condition where the circularity is detected.
     * @param attributeName  The user attribute name.
     * @param attributeValue The user attribute value.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getUserObjectAttributeIsAutoConverted(String key, UserCondition userCondition, String attributeName, String attributeValue) {
        return new FormattableLogMessageWithUserCondition("Evaluation of condition (%s) for setting '%s' may not produce the expected result (the User.%s attribute is not a string value, thus it was automatically converted to the string value '%s'). Please make sure that using a non-string value was intended.", userCondition, key, attributeName, attributeValue);
    }

    /**
     * Log message for Config Service Method Has No Effect Due To Closed Client warning. The log eventId 3201.
     *
     * @param methodName The method name.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getConfigServiceMethodHasNoEffectDueToClosedClient(final String methodName) {
        return new FormattableLogMessage("The client object is already closed, thus `%s` has no effect.", methodName);
    }

    /**
     * Log message for Auto Poll Max Init Wait Time Reached warning. The log eventId 4200.
     *
     * @param maxInitWaitTimeSeconds The auto polling `maxInitWaitTimeSeconds` value.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getAutoPollMaxInitWaitTimeReached(final int maxInitWaitTimeSeconds) {
        return new FormattableLogMessage("`maxInitWaitTimeSeconds` for the very first fetch reached (%ds). Returning cached config.", maxInitWaitTimeSeconds);
    }

    /**
     * Log message for Config Service Status Changed info. The log eventId 5200.
     *
     * @param mode The change mode.
     * @return The formattable log message.
     */
    public static FormattableLogMessage getConfigServiceStatusChanged(final String mode) {
        return new FormattableLogMessage("Switched to %s mode.", mode);
    }

}
