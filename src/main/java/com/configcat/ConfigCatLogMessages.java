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
    public static final String FETCH_FAILED_DUE_TO_UNEXPECTED_ERROR = "Unexpected error occurred while trying to fetch config JSON.";

    /**
     * Log message for Fetch Failed Due To Invalid Sdk Key error. The log eventId is 1100.
     */
    public static final String FETCH_FAILED_DUE_TO_INVALID_SDK_KEY_ERROR = "Your SDK Key seems to be wrong. You can find the valid SDK Key at https://app.configcat.com/sdkkey";

    private ConfigCatLogMessages() { /* prevent from instantiation*/ }

    /**
     * Log message for Config Json Is Not Presented errors when the method returns with default value. The log eventId is 1000.
     *
     * @param key The feature flag key.
     * @param defaultParamName  The default parameter name.
     * @param defaultParamValue The default parameter value.
     * @return The formatted error message.
     */
    public static String getConfigJsonIsNotPresentedWitDefaultValue(final String key, final String defaultParamName, final Object defaultParamValue) {
        return "Config JSON is not present when evaluating setting '" + key + "'. Returning the `" + defaultParamName + "` parameter that you specified in your application: '" + defaultParamValue + "'.";
    }

    /**
     * Log message for Config Json Is Not Presented errors when the method returns with empty value. The log eventId is 1000.
     *
     * @param emptyResult The empty result.
     * @return The formatted error message.
     */
    public static String getConfigJsonIsNotPresentedWitEmptyResult(final String emptyResult) {
        return "Config JSON is not present. Returning " + emptyResult + ".";
    }

    /**
     * Log message for Setting Evaluation Failed Due To Missing Key error. The log eventId is 1001.
     *
     * @param key               The feature flag key.
     * @param defaultParamName  The default parameter name.
     * @param defaultParamValue The default parameter value.
     * @param availableKeysSet  The set of available keys in the settings.
     * @return The formatted error message.
     */
    public static String getSettingEvaluationFailedDueToMissingKey(final String key, final String defaultParamName, final Object defaultParamValue, final Set<String> availableKeysSet) {
        return "Failed to evaluate setting '" + key + "' (the key was not found in config JSON). Returning the `" + defaultParamName + "` parameter that you specified in your application: '" + defaultParamValue + "'. Available keys: [" + availableKeysSet.stream().map(keyTo -> "'" + keyTo + "'").collect(Collectors.joining(",")) + "].";
    }

    /**
     * Log message for Setting Evaluation errors when the method returns with default value. The log eventId is 1002.
     *
     * @param methodName        The method name where the error is logged.
     * @param key               The feature flag key.
     * @param defaultParamName  The default parameter name.
     * @param defaultParamValue The default parameter value.
     * @return The formatted error message.
     */
    public static String getSettingEvaluationErrorWithDefaultValue(final String methodName, final String key, final String defaultParamName, final Object defaultParamValue) {
        return "Error occurred in the `" + methodName + "` method while evaluating setting '" + key + "'. Returning the `" + defaultParamName + "` parameter that you specified in your application: '" + defaultParamValue + "'.";
    }

    /**
     * Log message for Setting Evaluation errors when the method returns with empty value. The log eventId is 1002.
     *
     * @param methodName  The method name where the error is logged.
     * @param emptyResult The empty result.
     * @return The formatted error message.
     */
    public static String getSettingEvaluationErrorWithEmptyValue(final String methodName, final String emptyResult) {
        return "Error occurred in the `" + methodName + "` method. Returning " + emptyResult + ".";
    }

    /**
     * Log message for Force Refresh errors. The log eventId is 1003.
     *
     * @param methodName The method name where the error is logged.
     * @return The formatted error message.
     */
    public static String getForceRefreshError(final String methodName) {
        return "Error occurred in the `" + methodName + "` method.";
    }

    /**
     * Log message for Setting Evaluation Failed For Other Reason errors. The log eventId is 2001.
     *
     * @param key               The feature flag key.
     * @param defaultParamName  The default parameter name.
     * @param defaultParamValue The default parameter value.
     * @return The formatted error message.
     */
    public static String getSettingEvaluationFailedForOtherReason(final String key, final String defaultParamName, final Object defaultParamValue) {
        return "Failed to evaluate setting '" + key + "'. Returning the `" + defaultParamName + "` parameter that you specified in your application: '" + defaultParamValue + "'.";
    }

    /**
     * Log message for Setting For Variation Id Is Not Present error. The log eventId is 2011.
     *
     * @param variationId The variation id.
     * @return The formatted error message.
     */
    public static String getSettingForVariationIdIsNotPresent(final String variationId) {
        return "Could not find the setting for the specified variation ID: '" + variationId + "'.";
    }

    /**
     * Log message for Fetch Failed Due To Unexpected Http Response error. The log eventId is 1101.
     *
     * @param responseCode    The http response code.
     * @param responseMessage The http response message.
     * @return The formatted error message.
     */
    public static String getFetchFailedDueToUnexpectedHttpResponse(final int responseCode, final String responseMessage) {
        return "Unexpected HTTP response was received while trying to fetch config JSON: " + responseCode + " " + responseMessage;
    }

    /**
     * Log message for Fetch Failed Due To Request Timeout error. The log eventId is 1102.
     *
     * @param connectTimeoutMillis Connect timeout in milliseconds.
     * @param readTimeoutMillis    Read timeout in milliseconds.
     * @param writeTimeoutMillis   Write timeout in milliseconds.
     * @return The formatted error message.
     */
    public static String getFetchFailedDueToRequestTimeout(final Integer connectTimeoutMillis, final Integer readTimeoutMillis, final Integer writeTimeoutMillis) {
        return "Request timed out while trying to fetch config JSON. Timeout values: [connect: " + connectTimeoutMillis + "ms, read: " + readTimeoutMillis + "ms, write: " + writeTimeoutMillis + "ms]";
    }

    /**
     * Log message for Local File Data Source Does Not Exist error. The log eventId is 1300.
     *
     * @param filePath The file path.
     * @return The formatted error message.
     */
    public static String getLocalFileDataSourceDoesNotExist(final String filePath) {
        return "Cannot find the local config file '" + filePath + "'. This is a path that your application provided to the ConfigCat SDK by passing it to the `OverrideDataSourceBuilder.localFile()` method. Read more: https://configcat.com/docs/sdk-reference/java/#json-file";
    }

    /**
     * Log message for Local File Data Source Failed To Read File error. The log eventId is 1302.
     *
     * @param filePath The file path.
     * @return The formatted error message.
     */
    public static String getLocalFileDataSourceFailedToReadFile(final String filePath) {
        return "Failed to read the local config file '" + filePath + "'.";
    }

    /**
     * Log message for Client Is Already Created warning. The log eventId 3000.
     *
     * @param sdkKey The ConfigCat client SDK key.
     * @return The formatted warn message.
     */
    public static String getClientIsAlreadyCreated(final String sdkKey) {
        return "There is an existing client instance for the specified SDK Key. No new client instance will be created and the specified options callback is ignored. Returning the existing client instance. SDK Key: '" + sdkKey + "'.";
    }

    /**
     * Log message for Targeting Is Not Possible warning. The log eventId 3001.
     *
     * @param key The feature flag setting key.
     * @return The formatted warn message.
     */
    public static String getTargetingIsNotPossible(final String key) {
        return "Cannot evaluate targeting rules and % options for setting '" + key + "' (User Object is missing). You should pass a User Object to the evaluation methods like `getValue()`/`getValueAsync()` in order to make targeting work properly. Read more: https://configcat.com/docs/advanced/user-object/";
    }

    /**
     * Log message for Config Service Method Has No Effect Due To Closed Client warning. The log eventId 3201.
     *
     * @param methodName The method name.
     * @return The formatted warn message.
     */
    public static String getConfigServiceMethodHasNoEffectDueToClosedClient(final String methodName) {
        return "The client object is already closed, thus `" + methodName + "` has no effect.";
    }

    /**
     * Log message for Auto Poll Max Init Wait Time Reached warning. The log eventId 4200.
     *
     * @param maxInitWaitTimeSeconds The auto polling `maxInitWaitTimeSeconds` value.
     * @return The formatted warn message.
     */
    public static String getAutoPollMaxInitWaitTimeReached(final int maxInitWaitTimeSeconds) {
        return "`maxInitWaitTimeSeconds` for the very first fetch reached (" + maxInitWaitTimeSeconds + "s). Returning cached config.";
    }

    /**
     * Log message for Config Service Status Changed info. The log eventId 5200.
     *
     * @param mode The change mode.
     * @return The formatted info message.
     */
    public static String getConfigServiceStatusChanged(final String mode) {
        return "Switched to " + mode + " mode.";
    }

}
