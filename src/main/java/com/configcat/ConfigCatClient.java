package com.configcat;

import okhttp3.OkHttpClient;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A client for handling configurations provided by ConfigCat.
 */
public final class ConfigCatClient implements ConfigurationProvider {
    private static final String BASE_URL_GLOBAL = "https://cdn-global.configcat.com";
    private static final String BASE_URL_EU = "https://cdn-eu.configcat.com";
    private static final Map<String, ConfigCatClient> INSTANCES = new HashMap<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ConfigCatLogger logger;
    private final RolloutEvaluator rolloutEvaluator;
    private final OverrideDataSource overrideDataSource;
    private final OverrideBehaviour overrideBehaviour;
    private final String sdkKey;
    private User defaultUser;
    private ConfigService configService;
    private final ConfigCatHooks configCatHooks;


    private ConfigCatClient(String sdkKey, Options options){
        this.logger = new ConfigCatLogger(LoggerFactory.getLogger(ConfigCatClient.class), options.logLevel, options.configCatHooks);

        this.sdkKey = sdkKey;
        this.overrideDataSource = options.localDataSourceBuilder != null
                ? options.localDataSourceBuilder.build(this.logger)
                : new OverrideDataSource();
        this.overrideBehaviour = options.overrideBehaviour;
        this.rolloutEvaluator = new RolloutEvaluator(this.logger);
        this.configCatHooks = options.configCatHooks;

        if (this.overrideBehaviour != OverrideBehaviour.LOCAL_ONLY) {
            ConfigFetcher fetcher = new ConfigFetcher(options.httpClient == null
                    ? new OkHttpClient
                    .Builder()
                    .build()
                    : options.httpClient,
                    this.logger,
                    sdkKey,
                    !options.isBaseURLCustom()
                            ? options.dataGovernance == DataGovernance.GLOBAL
                            ? BASE_URL_GLOBAL
                            : BASE_URL_EU
                            : options.baseUrl,
                    options.isBaseURLCustom(),
                    options.pollingMode.getPollingIdentifier());

            this.configService = new ConfigService(sdkKey, fetcher, options.pollingMode, options.cache, logger, options.offline, options.configCatHooks);
        }

        this.defaultUser = options.defaultUser;
    }

    @Override
    public <T> T getValue(Class<T> classOfT, String key, T defaultValue) {
        return this.getValue(classOfT, key, null, defaultValue);
    }

    @Override
    public <T> T getValue(Class<T> classOfT, String key, User user, T defaultValue) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("'key' cannot be null or empty.");

        if (classOfT != String.class &&
                classOfT != Integer.class &&
                classOfT != int.class &&
                classOfT != Double.class &&
                classOfT != double.class &&
                classOfT != Boolean.class &&
                classOfT != boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported.");

        try {
            return this.getValueAsync(classOfT, key, user, defaultValue).get();
        } catch (InterruptedException e) {
            this.logger.error(0, "Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return defaultValue;
        } catch (Exception e) {
            this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithDefaultValue("getValue", key, "defaultValue", defaultValue.toString()), e);
            return defaultValue;
        }
    }

    @Override
    public <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, T defaultValue) {
        return this.getValueAsync(classOfT, key, null, defaultValue);
    }

    @Override
    public <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, User user, T defaultValue) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("'key' cannot be null or empty.");

        if (classOfT != String.class &&
                classOfT != Integer.class &&
                classOfT != int.class &&
                classOfT != Double.class &&
                classOfT != double.class &&
                classOfT != Boolean.class &&
                classOfT != boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported.");

        return this.getSettingsAsync()
                .thenApply(settingResult -> this.getValueFromSettingsMap(classOfT, settingResult, key, user, defaultValue));
    }

    @Override
    public <T> EvaluationDetails<T> getValueDetails(Class<T> classOfT, String key, T defaultValue) {
        return this.getValueDetails(classOfT, key, null, defaultValue);
    }

    @Override
    public <T> EvaluationDetails<T> getValueDetails(Class<T> classOfT, String key, User user, T defaultValue) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("'key' cannot be null or empty.");

        if (classOfT != String.class &&
                classOfT != Integer.class &&
                classOfT != int.class &&
                classOfT != Double.class &&
                classOfT != double.class &&
                classOfT != Boolean.class &&
                classOfT != boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported.");

        try {
            return this.getValueDetailsAsync(classOfT, key, user, defaultValue).get();
        } catch (InterruptedException e) {
            String error = "Thread interrupted.";
            this.logger.error(0, error, e);
            Thread.currentThread().interrupt();
            return EvaluationDetails.fromError(key, defaultValue, error + ": " + e.getMessage(), user);
        } catch (Exception e) {
            this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithDefaultValue("getValueDetails", key, "defaultValue", defaultValue), e);
            return EvaluationDetails.fromError(key, defaultValue, e.getMessage(), user);
        }
    }

    @Override
    public <T> CompletableFuture<EvaluationDetails<T>> getValueDetailsAsync(Class<T> classOfT, String key, T defaultValue) {
        return this.getValueDetailsAsync(classOfT, key, null, defaultValue);
    }

    @Override
    public <T> CompletableFuture<EvaluationDetails<T>> getValueDetailsAsync(Class<T> classOfT, String key, User user, T defaultValue) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("'key' cannot be null or empty.");

        if (classOfT != String.class &&
                classOfT != Integer.class &&
                classOfT != int.class &&
                classOfT != Double.class &&
                classOfT != double.class &&
                classOfT != Boolean.class &&
                classOfT != boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported.");

        return this.getSettingsAsync()
                .thenApply(settingsResult -> {
                    Result<Setting> checkSettingResult = checkSettingAvailable(settingsResult, key, defaultValue);
                    if (checkSettingResult.error() != null) {
                        EvaluationDetails<Object> evaluationDetails = EvaluationDetails.fromError(key, defaultValue, checkSettingResult.error(), user);
                        this.configCatHooks.invokeOnFlagEvaluated(evaluationDetails);
                        return evaluationDetails.asTypeSpecific();
                    }

                    return this.evaluate(classOfT, checkSettingResult.value(),
                            key, user != null ? user : this.defaultUser, settingsResult.fetchTime());
                });
    }

    @Override
    public Map<String, Object> getAllValues(User user) {
        try {
            return this.getAllValuesAsync(user).get();
        } catch (InterruptedException e) {
            this.logger.error(0, "Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return new HashMap<>();
        } catch (Exception e) {
            this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithEmptyValue("getAllValues", "empty map"), e);
            return new HashMap<>();
        }
    }

    @Override
    public CompletableFuture<Map<String, Object>> getAllValuesAsync(User user) {
        return this.getSettingsAsync()
                .thenApply(settingResult -> {
                    try {
                        if (!checkSettingsAvailable(settingResult, "empty map")) {
                            return new HashMap<>();
                        }

                        Map<String, Setting> settings = settingResult.settings();
                        Collection<String> keys = settings.keySet();
                        Map<String, Object> result = new HashMap<>();

                        for (String key : keys) {
                            Setting setting = settings.get(key);

                            SettingsValue evaluated = this.rolloutEvaluator.evaluate(setting, key, getEvaluateUser(user)).value;
                            Object value = this.parseObject(this.classBySettingType(setting.getType()), evaluated, setting.getType());
                            result.put(key, value);
                        }

                        return result;
                    } catch (Exception e) {
                        this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithEmptyValue("getAllValuesAsync", "empty map"), e);
                        return new HashMap<>();
                    }
                });
    }

    @Override
    public List<EvaluationDetails<Object>> getAllValueDetails(User user) {
        try {
            return this.getAllValueDetailsAsync(user).get();
        } catch (InterruptedException e) {
            this.logger.error(0, "Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (Exception e) {
            this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithEmptyValue("getAllValueDetails", "empty list"), e);
            return new ArrayList<>();
        }
    }

    @Override
    public CompletableFuture<List<EvaluationDetails<Object>>> getAllValueDetailsAsync(User user) {
        return this.getSettingsAsync()
                .thenApply(settingResult -> {
                    try {
                        if (!checkSettingsAvailable(settingResult, "empty list")) {
                            return new ArrayList<>();
                        }

                        Map<String, Setting> settings = settingResult.settings();
                        List<EvaluationDetails<Object>> result = new ArrayList<>();

                        for (String key : settings.keySet()) {
                            Setting setting = settings.get(key);

                            EvaluationDetails<Object> evaluationDetails = this.evaluateObject(this.classBySettingType(setting.getType()), setting,
                                    key, user != null ? user : this.defaultUser, settingResult.fetchTime());
                            result.add(evaluationDetails);
                        }

                        return result;
                    } catch (Exception e) {
                        this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithEmptyValue("getAllValueDetailsAsync", "empty list"), e);
                        return new ArrayList<>();
                    }
                });
    }

    @Override
    public <T> Map.Entry<String, T> getKeyAndValue(Class<T> classOfT, String variationId) {
        if (variationId == null || variationId.isEmpty())
            throw new IllegalArgumentException("'variationId' cannot be null or empty.");

        try {
            return this.getKeyAndValueAsync(classOfT, variationId).get();
        } catch (InterruptedException e) {
            this.logger.error(0, "Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithEmptyValue("getKeyAndValue", "null"), e);
            return null;
        }
    }

    @Override
    public <T> CompletableFuture<Map.Entry<String, T>> getKeyAndValueAsync(Class<T> classOfT, String variationId) {
        if (variationId == null || variationId.isEmpty())
            throw new IllegalArgumentException("'variationId' cannot be null or empty.");

        return this.getSettingsAsync()
                .thenApply(settingsResult -> this.getKeyAndValueFromSettingsMap(classOfT, settingsResult, variationId));
    }

    @Override
    public Collection<String> getAllKeys() {
        try {
            return this.getAllKeysAsync().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.logger.error(0, "Thread interrupted.", e);
            return new ArrayList<>();
        } catch (Exception e) {
            this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithEmptyValue("getAllKeys", "empty array"), e);
            return new ArrayList<>();
        }
    }

    @Override
    public CompletableFuture<Collection<String>> getAllKeysAsync() {
        return this.getSettingsAsync()
                .thenApply(settingResult -> {
                    try {
                        if (!checkSettingsAvailable(settingResult, "empty array")) {
                            return new ArrayList<>();
                        }

                        return settingResult.settings().keySet();
                    } catch (Exception e) {
                        this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithEmptyValue("getAllKeysAsync", "empty array"), e);
                        return new ArrayList<>();
                    }
                });
    }

    @Override
    public RefreshResult forceRefresh() {
        try {
            return this.forceRefreshAsync().get();
        } catch (InterruptedException e) {
            this.logger.error(0, "Thread interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            this.logger.error(1003, ConfigCatLogMessages.getForceRefreshError("forceRefresh"), e);
        }
        return new RefreshResult(false, "An error occurred during the refresh.");
    }

    @Override
    public CompletableFuture<RefreshResult> forceRefreshAsync() {
        return this.configService.refresh();
    }

    @Override
    public void setDefaultUser(User defaultUser) {
        if (isClosed()) {
            logger.warn(3201, ConfigCatLogMessages.getConfigServiceMethodHasNoEffectDueToClosedClient("setDefaultUser"));
            return;
        }
        this.defaultUser = defaultUser;
    }

    @Override
    public void clearDefaultUser() {
        if (isClosed()) {
            logger.warn(3201, ConfigCatLogMessages.getConfigServiceMethodHasNoEffectDueToClosedClient("clearDefaultUser"));
            return;
        }
        this.defaultUser = null;
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public void setOnline() {
        if (this.configService != null && !isClosed()) {
            this.configService.setOnline();
        } else {
            logger.warn(3201, ConfigCatLogMessages.getConfigServiceMethodHasNoEffectDueToClosedClient("setOnline"));
        }
    }

    @Override
    public void setOffline() {
        if (this.configService != null && !isClosed()) {
            this.configService.setOffline();
        } else {
            logger.warn(3201, ConfigCatLogMessages.getConfigServiceMethodHasNoEffectDueToClosedClient("setOffline"));
        }
    }

    @Override
    public boolean isOffline() {
        if (configService == null) {
            return true;
        }
        return configService.isOffline();
    }

    @Override
    public ConfigCatHooks getHooks() {
        return this.configCatHooks;
    }

    @Override
    public void close() throws IOException {
        if (!this.isClosed.compareAndSet(false, true)) {
            return;
        }
        synchronized (INSTANCES) {
            ConfigCatClient client = INSTANCES.get(sdkKey);
            if (client != null) {
                if (!this.equals(client)) {
                    return;
                }
                closeResources();
                INSTANCES.remove(sdkKey);
            } else {
                // if client created with deprecated constructor, just close resources
                closeResources();
            }
        }
    }

    private void closeResources() throws IOException {
        if (configService != null) {
            this.configService.close();
        }
        this.overrideDataSource.close();
        this.configCatHooks.clear();
    }

    /**
     * Close all ConfigCatClient instances.
     *
     * @throws IOException If client resource close fails.
     */
    public static void closeAll() throws IOException {
        synchronized (INSTANCES) {
            for (ConfigCatClient client : INSTANCES.values()) {
                client.closeResources();
            }
            INSTANCES.clear();
        }
    }

    private CompletableFuture<SettingResult> getSettingsAsync() {
        if (this.overrideBehaviour != null) {
            switch (this.overrideBehaviour) {
                case LOCAL_ONLY:
                    return CompletableFuture.completedFuture(new SettingResult(this.overrideDataSource.getLocalConfiguration(), Constants.DISTANT_PAST));
                case REMOTE_OVER_LOCAL:
                    if (configService == null)
                        return CompletableFuture.completedFuture(new SettingResult(this.overrideDataSource.getLocalConfiguration(), Constants.DISTANT_PAST));
                    return this.configService.getSettings()
                            .thenApply(settingResult -> {
                                Map<String, Setting> localSettings = new HashMap<>(this.overrideDataSource.getLocalConfiguration());
                                localSettings.putAll(settingResult.settings());
                                return new SettingResult(localSettings, settingResult.fetchTime());
                            });
                case LOCAL_OVER_REMOTE:
                    if (configService == null)
                        return CompletableFuture.completedFuture(new SettingResult(this.overrideDataSource.getLocalConfiguration(), Constants.DISTANT_PAST));
                    return this.configService.getSettings()
                            .thenApply(settingResult -> {
                                Map<String, Setting> localSettings = this.overrideDataSource.getLocalConfiguration();
                                Map<String, Setting> remoteSettings = new HashMap<>(settingResult.settings());
                                remoteSettings.putAll(localSettings);
                                return new SettingResult(remoteSettings, settingResult.fetchTime());
                            });
            }
        }

        return configService == null
                ? CompletableFuture.completedFuture(SettingResult.EMPTY)
                : configService.getSettings();
    }

    private boolean checkSettingsAvailable(SettingResult settingResult, String emptyResult) {
        if (settingResult.isEmpty()) {
            this.logger.error(1000, ConfigCatLogMessages.getConfigJsonIsNotPresentedWithEmptyResult(emptyResult));
            return false;
        }

        return true;
    }

    private <T> Result<Setting> checkSettingAvailable(SettingResult settingResult, String key, T defaultValue) {
        if (settingResult.isEmpty()) {
            String errorMessage = ConfigCatLogMessages.getConfigJsonIsNotPresentedWithDefaultValue(key, "defaultValue", defaultValue);
            this.logger.error(1000, errorMessage);
            return Result.error(errorMessage, null);
        }

        Map<String, Setting> settings = settingResult.settings();
        Setting setting = settings.get(key);
        if (setting == null) {
            String errorMessage = ConfigCatLogMessages.getSettingEvaluationFailedDueToMissingKey(key, "defaultValue", defaultValue, settings.keySet());
            this.logger.error(1001, errorMessage);
            return Result.error(errorMessage, null);
        }

        return Result.success(setting);
    }

    private <T> T getValueFromSettingsMap(Class<T> classOfT, SettingResult settingResult, String key, User user, T defaultValue) {
        try {
            Result<Setting> checkSettingResult = checkSettingAvailable(settingResult, key, defaultValue);
            if (checkSettingResult.error() != null) {
                this.configCatHooks.invokeOnFlagEvaluated(EvaluationDetails.fromError(key, defaultValue, checkSettingResult.error(), user));
                return defaultValue;
            }

            return this.evaluate(classOfT, checkSettingResult.value(), key, getEvaluateUser(user), settingResult.fetchTime()).getValue();
        } catch (Exception e) {
            String errorMessage = ConfigCatLogMessages.getSettingEvaluationFailedForOtherReason(key, "defaultValue", defaultValue);
            this.logger.error(2001, errorMessage, e);
            this.configCatHooks.invokeOnFlagEvaluated(EvaluationDetails.fromError(key, defaultValue, errorMessage + " " + e.getMessage(), user));
            return defaultValue;
        }
    }

    private <T> Map.Entry<String, T> getKeyAndValueFromSettingsMap(Class<T> classOfT, SettingResult settingResult, String variationId) {
        try {
            if (!checkSettingsAvailable(settingResult, "null")) {
                return null;
            }

            Map<String, Setting> settings = settingResult.settings();
            for (Map.Entry<String, Setting> node : settings.entrySet()) {
                String settingKey = node.getKey();
                Setting setting = node.getValue();
                if (variationId.equals(setting.getVariationId())) {
                    return new AbstractMap.SimpleEntry<>(settingKey, (T) this.parseObject(classOfT, setting.getSettingsValue(), setting.getType()));
                }

                for (TargetingRule rolloutRule : setting.getTargetingRules()) {
                    if (variationId.equals(rolloutRule.getServedValue().getVariationId())) {
                        return new AbstractMap.SimpleEntry<>(settingKey, (T) this.parseObject(classOfT, rolloutRule.getServedValue().getValue(), setting.getType()));
                    }
                }

                for (PercentageOption percentageRule : setting.getPercentageOptions()) {
                    if (variationId.equals(percentageRule.getVariationId())) {
                        return new AbstractMap.SimpleEntry<>(settingKey, (T) this.parseObject(classOfT, percentageRule.getValue(), setting.getType()));
                    }
                }
            }

            this.logger.error(2011, ConfigCatLogMessages.getSettingForVariationIdIsNotPresent(variationId));
            return null;
        } catch (Exception e) {
            this.logger.error(1002, ConfigCatLogMessages.getSettingEvaluationErrorWithEmptyValue("getKeyAndValueFromSettingsMap", "null"), e);
            return null;
        }
    }

    private Object parseObject(Class<?> classOfT, SettingsValue settingsValue, SettingType settingType) {
        if(!validateParseType(classOfT))
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");

        if (classOfT == String.class && settingsValue.getStringValue() != null && SettingType.STRING.equals(settingType))
            return settingsValue.getStringValue();
        else if ((classOfT == Integer.class || classOfT == int.class) && settingsValue.getIntegerValue() != null && SettingType.INT.equals(settingType))
            return settingsValue.getIntegerValue();
        else if ((classOfT == Double.class || classOfT == double.class) && settingsValue.getDoubleValue() != null && SettingType.DOUBLE.equals(settingType))
            return settingsValue.getDoubleValue();
        else if ((classOfT == Boolean.class || classOfT == boolean.class) && settingsValue.getBooleanValue() !=  null && SettingType.BOOLEAN.equals(settingType))
            return settingsValue.getBooleanValue();
        else
            throw new IllegalArgumentException("The type of a setting must match the type of the setting's default value. "
                    + "Setting's type was {" + settingType + "} but the default value's type was {" + classOfT + "}. "
                    + "Please use a default value which corresponds to the setting type {" + settingType + "}.");
    }

    private boolean validateParseType(Class<?> classOfT){
        if (classOfT == String.class || classOfT == Integer.class || classOfT == int.class || classOfT == Double.class || classOfT == double.class || classOfT == Boolean.class || classOfT == boolean.class){
            return true;
        }
        return false;
    }

    private Class<?> classBySettingType(SettingType settingType) {
        if (settingType == SettingType.BOOLEAN)
            return boolean.class;
        else if (settingType == SettingType.STRING)
            return String.class;
        else if (settingType == SettingType.INT)
            return int.class;
        else if (settingType == SettingType.DOUBLE)
            return double.class;
        else
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");
    }

    /**
     * Checks the user for evaluation, if the user null return with the default user .
     *
     * @param user The user for evaluation.
     * @return if the user null return with the default user else with the user.
     */
    private User getEvaluateUser(final User user) {
        return user != null ? user : defaultUser;
    }

    /**
     * Get a singleton ConfigCat Client for the sdk key, create a new client if not existed yet.
     *
     * @param sdkKey the client sdk key.
     * @return a singleton client.
     */
    public static ConfigCatClient get(final String sdkKey) {
        return ConfigCatClient.get(sdkKey, null);
    }

    /**
     * Creates a new or gets an already existing ConfigCatClient for the given sdkKey.
     *
     * @param sdkKey          the SDK Key for to communicate with the ConfigCat services.
     * @param optionsCallback the options callback to set up the created ConfigCatClient instance.
     * @return the ConfigCatClient instance.
     */
    public static ConfigCatClient get(String sdkKey, Consumer<Options> optionsCallback) {
        if (sdkKey == null || sdkKey.isEmpty()) {
            throw new IllegalArgumentException("SDK Key cannot be null or empty.");
        }
        Options clientOptions = new Options();

        if (optionsCallback != null) {
            Options options = new Options();
            optionsCallback.accept(options);
            clientOptions = options;
        }

        if (!OverrideBehaviour.LOCAL_ONLY.equals(clientOptions.overrideBehaviour) && !isValidKey(sdkKey, clientOptions.isBaseURLCustom())) {
            throw new IllegalArgumentException("SDK Key '" + sdkKey + "' is invalid.");
        }

        synchronized (INSTANCES) {
            ConfigCatClient client = INSTANCES.get(sdkKey);
            if (client != null) {
                if (optionsCallback != null) {
                    client.logger.warn(3000, ConfigCatLogMessages.getClientIsAlreadyCreated(sdkKey));
                }
                return client;
            }
            client = new ConfigCatClient(sdkKey, clientOptions);
            INSTANCES.put(sdkKey, client);

            return client;
        }
    }

    private static boolean isValidKey(final String sdkKey, final boolean isCustomBaseURL) {
        //configcat-proxy/ rules
        if (isCustomBaseURL && sdkKey.length() > Constants.SDK_KEY_PROXY_PREFIX.length() && sdkKey.startsWith(Constants.SDK_KEY_PROXY_PREFIX)) {
            return true;
        }
        String[] splitSDKKey = sdkKey.split("/");
        //22/22 rules
        if (splitSDKKey.length == 2 && splitSDKKey[0].length() == Constants.SDK_KEY_SECTION_LENGTH && splitSDKKey[1].length() == Constants.SDK_KEY_SECTION_LENGTH) {
            return true;
        }
        //configcat-sdk-1/22/22 rules
        return splitSDKKey.length == 3 && splitSDKKey[0].equals(Constants.SDK_KEY_PREFIX) && splitSDKKey[1].length() == Constants.SDK_KEY_SECTION_LENGTH && splitSDKKey[2].length() == Constants.SDK_KEY_SECTION_LENGTH;
    }

    private EvaluationDetails<Object> evaluateObject(Class classOfT, Setting setting, String key, User user, Long fetchTime) {
        EvaluationResult evaluationResult = this.rolloutEvaluator.evaluate(setting, key, user);
        EvaluationDetails<Object> details = new EvaluationDetails<>(
                this.parseObject(classOfT, evaluationResult.value, setting.getType()),
                key,
                evaluationResult.variationId,
                user,
                false,
                null,
                fetchTime,
                evaluationResult.targetingRule,
                evaluationResult.percentageOption);
        this.configCatHooks.invokeOnFlagEvaluated(details);
        return details;
    }

    private <T> EvaluationDetails<T> evaluate(Class<T> classOfT, Setting setting, String key, User user, Long fetchTime) {
        return evaluateObject(classOfT, setting, key, user, fetchTime).asTypeSpecific();
    }

    /**
     * Options for configuring  {@link ConfigCatClient} instance.
     */
    public static class Options {
        private OkHttpClient httpClient;
        private ConfigCache cache = new NullConfigCache();
        private String baseUrl;
        private PollingMode pollingMode = PollingModes.autoPoll();
        private LogLevel logLevel = LogLevel.WARNING;
        private DataGovernance dataGovernance = DataGovernance.GLOBAL;
        private OverrideDataSourceBuilder localDataSourceBuilder;
        private OverrideBehaviour overrideBehaviour;
        private User defaultUser;
        private boolean offline = false;
        private final ConfigCatHooks configCatHooks = new ConfigCatHooks();


        /**
         * Sets the underlying http client which will be used to fetch the latest configuration.
         *
         * @param httpClient the http client.
         */
        public void httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
        }

        /**
         * Sets the internal cache implementation.
         *
         * @param cache a {@link ConfigCache} implementation used to cache the configuration.
         */
        public void cache(ConfigCache cache) {
            this.cache = cache;
        }

        /**
         * Sets the base ConfigCat CDN url.
         *
         * @param baseUrl the base ConfigCat CDN url.
         */
        public void baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * Sets the internal refresh policy implementation.
         *
         * @param pollingMode the polling mode.
         */
        public void pollingMode(PollingMode pollingMode) {
            this.pollingMode = pollingMode;
        }

        /**
         * Default: Global. Set this parameter to be in sync with the Data Governance preference on the Dashboard:
         * https://app.configcat.com/organization/data-governance (Only Organization Admins have access)
         *
         * @param dataGovernance the {@link DataGovernance} parameter.
         */
        public void dataGovernance(DataGovernance dataGovernance) {
            this.dataGovernance = dataGovernance;
        }

        /**
         * Default: Warning. Sets the internal log level.
         *
         * @param logLevel the {@link LogLevel} parameter.
         */
        public void logLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
        }

        /**
         * Sets feature flag and setting overrides.
         *
         * @param dataSourceBuilder builder that describes the overrides' data source.
         * @param behaviour         the override behaviour. It can be used to set preference on whether the local
         *                          values should override the remote values, or use local values only when a remote
         *                          value doesn't exist, or use it for local only mode.
         * @throws IllegalArgumentException when the dataSourceBuilder or behaviour parameter is null.
         */
        public void flagOverrides(OverrideDataSourceBuilder dataSourceBuilder, OverrideBehaviour behaviour) {
            if (dataSourceBuilder == null) {
                throw new IllegalArgumentException("'dataSourceBuilder' cannot be null or empty.");
            }

            if (behaviour == null) {
                throw new IllegalArgumentException("'behaviour' cannot be null.");
            }

            this.localDataSourceBuilder = dataSourceBuilder;
            this.overrideBehaviour = behaviour;
        }

        /**
         * Sets the default user.
         *
         * @param defaultUser the default user.
         */
        public void defaultUser(User defaultUser) {
            this.defaultUser = defaultUser;
        }

        /**
         * Set client offline mode.
         *
         * @param offline the client offline mode value.
         */
        public void offline(boolean offline) {
            this.offline = offline;
        }

        /**
         * Set the client hooks.
         *
         * @return the hooks.
         */
        public ConfigCatHooks hooks() {
            return configCatHooks;
        }

        private boolean isBaseURLCustom() {
            return this.baseUrl != null && !this.baseUrl.isEmpty();
        }
    }
}