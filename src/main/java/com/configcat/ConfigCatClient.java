package com.configcat;

import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A client for handling configurations provided by ConfigCat.
 */
public final class ConfigCatClient implements ConfigurationProvider {
    private static final String BASE_URL_GLOBAL = "https://cdn-global.configcat.com";
    private static final String BASE_URL_EU = "https://cdn-eu.configcat.com";
    private static final Map<String, ConfigCatClient> INSTANCES = new HashMap<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final RefreshPolicy refreshPolicy;
    private final ConfigCatLogger logger;
    private final RolloutEvaluator rolloutEvaluator;
    private final OverrideDataSource overrideDataSource;
    private final OverrideBehaviour overrideBehaviour;
    private final String sdkKey;
    private User defaultUser;


    private ConfigCatClient(String sdkKey, Options options) throws IllegalArgumentException {
        if (sdkKey == null || sdkKey.isEmpty())
            throw new IllegalArgumentException("'sdkKey' cannot be null or empty.");

        LogLevel logLevel = options.logLevel == null ? LogLevel.WARNING : options.logLevel;
        DataGovernance dataGovernance = options.dataGovernance == null ? DataGovernance.GLOBAL : options.dataGovernance;
        this.logger = new ConfigCatLogger(LoggerFactory.getLogger(ConfigCatClient.class), logLevel);

        this.sdkKey = sdkKey;
        this.overrideDataSource = options.localDataSourceBuilder != null
                ? options.localDataSourceBuilder.build(this.logger)
                : new OverrideDataSource();
        this.overrideBehaviour = options.overrideBehaviour;
        this.rolloutEvaluator = new RolloutEvaluator(this.logger);

        ConfigCache cache = options.cache == null
                ? new NullConfigCache()
                : options.cache;

        ConfigJsonCache configJsonCache = new ConfigJsonCache(this.logger, cache, sdkKey);

        PollingMode pollingMode = options.pollingMode == null
                ? PollingModes.autoPoll(60)
                : options.pollingMode;

        if (this.overrideBehaviour == OverrideBehaviour.LOCAL_ONLY) {
            this.refreshPolicy = new NullRefreshPolicy();
        } else {
            boolean hasCustomBaseUrl = options.baseUrl != null && !options.baseUrl.isEmpty();
            ConfigFetcher fetcher = new ConfigFetcher(options.httpClient == null
                    ? new OkHttpClient
                    .Builder()
                    .build()
                    : options.httpClient,
                    this.logger,
                    configJsonCache,
                    sdkKey,
                    !hasCustomBaseUrl
                            ? dataGovernance == DataGovernance.GLOBAL
                            ? BASE_URL_GLOBAL
                            : BASE_URL_EU
                            : options.baseUrl,
                    hasCustomBaseUrl,
                    pollingMode.getPollingIdentifier());

            this.refreshPolicy = this.selectPolicy(pollingMode, fetcher, this.logger, configJsonCache);
        }
        this.defaultUser = options.defaultUser;
    }

    /**
     * Constructs a new client instance with the default configuration.
     *
     * @param sdkKey the token which identifies your project configuration.
     * @deprecated Use the singleton client creation {@link ConfigCatClient#get(String)}
     */
    @Deprecated
    public ConfigCatClient(String sdkKey) {
        this(sdkKey, new Options());
        if (INSTANCES.containsKey(sdkKey)) {
            this.logger.warn("A singleton ConfigCat Client is already initialized with SDK Key '" + sdkKey + "'.");
        }
        this.logger.warn("We strongly recommend you to use the ConfigCat Client as a Singleton object in your application.");
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
            this.logger.error("Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return defaultValue;
        } catch (Exception e) {
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
                .thenApply(settings -> this.getValueFromSettingsMap(classOfT, settings, key, user, defaultValue));
    }

    @Override
    public String getVariationId(String key, String defaultVariationId) {
        return this.getVariationId(key, null, defaultVariationId);
    }

    @Override
    public String getVariationId(String key, User user, String defaultVariationId) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("'key' cannot be null or empty.");

        try {
            return this.getVariationIdAsync(key, user, defaultVariationId).get();
        } catch (InterruptedException e) {
            this.logger.error("Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return defaultVariationId;
        } catch (Exception e) {
            return defaultVariationId;
        }
    }

    @Override
    public CompletableFuture<String> getVariationIdAsync(String key, String defaultVariationId) {
        return this.getVariationIdAsync(key, null, defaultVariationId);
    }

    @Override
    public CompletableFuture<String> getVariationIdAsync(String key, User user, String defaultVariationId) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("'key' cannot be null or empty.");

        return this.getSettingsAsync()
                .thenApply(settings -> this.getVariationIdFromSettingsMap(settings, key, user, defaultVariationId));
    }

    @Override
    public Collection<String> getAllVariationIds() {
        return this.getAllVariationIds(null);
    }

    @Override
    public CompletableFuture<Collection<String>> getAllVariationIdsAsync() {
        return this.getAllVariationIdsAsync(null);
    }

    @Override
    public Collection<String> getAllVariationIds(User user) {
        try {
            return this.getAllVariationIdsAsync(user).get();
        } catch (InterruptedException e) {
            this.logger.error("Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (Exception e) {
            this.logger.error("An error occurred during getting all the variation ids. Returning empty array.", e);
            return new ArrayList<>();
        }
    }

    @Override
    public CompletableFuture<Collection<String>> getAllVariationIdsAsync(User user) {
        return this.getSettingsAsync()
                .thenApply(settings -> {
                    try {
                        Collection<String> keys = settings.keySet();
                        ArrayList<String> result = new ArrayList<>();

                        for (String key : keys) {
                            result.add(this.getVariationIdFromSettingsMap(settings, key, user, null));
                        }

                        return result;
                    } catch (Exception e) {
                        this.logger.error("An error occurred during getting all the variation ids. Returning empty array.", e);
                        return new ArrayList<>();
                    }
                });
    }

    @Override
    public Map<String, Object> getAllValues(User user) {
        try {
            return this.getAllValuesAsync(user).get();
        } catch (InterruptedException e) {
            this.logger.error("Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return new HashMap<>();
        } catch (Exception e) {
            this.logger.error("An error occurred during getting all values. Returning empty map.", e);
            return new HashMap<>();
        }
    }

    @Override
    public CompletableFuture<Map<String, Object>> getAllValuesAsync(User user) {
        return this.getSettingsAsync()
                .thenApply(settings -> {
                    try {
                        Collection<String> keys = settings.keySet();
                        Map<String, Object> result = new HashMap<>();

                        for (String key : keys) {
                            Setting setting = settings.get(key);

                            JsonElement evaluated = this.rolloutEvaluator.evaluate(setting, key, getEvaluateUser(user)).getKey();
                            Object value = this.parseObject(this.classBySettingType(setting.type), evaluated);
                            result.put(key, value);
                        }

                        return result;
                    } catch (Exception e) {
                        this.logger.error("An error occurred during getting all values. Returning empty map.", e);
                        return new HashMap<>();
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
            this.logger.error("Thread interrupted.", e);
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> CompletableFuture<Map.Entry<String, T>> getKeyAndValueAsync(Class<T> classOfT, String variationId) {
        if (variationId == null || variationId.isEmpty())
            throw new IllegalArgumentException("'variationId' cannot be null or empty.");

        return this.getSettingsAsync()
                .thenApply(settings -> this.getKeyAndValueFromSettingsMap(classOfT, settings, variationId));
    }

    @Override
    public Collection<String> getAllKeys() {
        try {
            return this.getAllKeysAsync().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.logger.error("Thread interrupted.", e);
            return new ArrayList<>();
        } catch (Exception e) {
            this.logger.error("An error occurred during getting all the setting keys. Returning empty array.", e);
            return new ArrayList<>();
        }
    }

    @Override
    public CompletableFuture<Collection<String>> getAllKeysAsync() {
        return this.getSettingsAsync()
                .thenApply(settings -> {
                    try {
                        return settings.keySet();
                    } catch (Exception e) {
                        this.logger.error("An error occurred during getting all the setting keys. Returning empty array.", e);
                        return new ArrayList<>();
                    }
                });
    }

    @Override
    public void forceRefresh() {
        try {
            this.forceRefreshAsync().get();
        } catch (InterruptedException e) {
            this.logger.error("Thread interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            this.logger.error("An error occurred during the refresh.", e);
        }
    }

    @Override
    public CompletableFuture<Void> forceRefreshAsync() {
        return this.refreshPolicy.refreshAsync();
    }

    public void setDefaultUser(User defaultUser) {
        this.defaultUser = defaultUser;
    }

    @Override
    public void clearDefaultUser() {
        this.defaultUser = null;
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
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
        this.refreshPolicy.close();
        this.overrideDataSource.close();
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

    private CompletableFuture<Map<String, Setting>> getSettingsAsync() {
        if (this.overrideBehaviour != null) {
            switch (this.overrideBehaviour) {
                case LOCAL_ONLY:
                    return CompletableFuture.completedFuture(this.overrideDataSource.getLocalConfiguration());
                case REMOTE_OVER_LOCAL:
                    return this.refreshPolicy.getSettingsAsync()
                            .thenApply(settings -> {
                                Map<String, Setting> localSettings = new HashMap<>(this.overrideDataSource.getLocalConfiguration());
                                localSettings.putAll(settings);
                                return localSettings;
                            });
                case LOCAL_OVER_REMOTE:
                    return this.refreshPolicy.getSettingsAsync()
                            .thenApply(settings -> {
                                Map<String, Setting> localSettings = this.overrideDataSource.getLocalConfiguration();
                                Map<String, Setting> remoteSettings = new HashMap<>(settings);
                                remoteSettings.putAll(localSettings);
                                return remoteSettings;
                            });
            }
        }

        return this.refreshPolicy.getSettingsAsync();
    }

    private <T> T getValueFromSettingsMap(Class<T> classOfT, Map<String, Setting> settings, String key, User user, T defaultValue) {
        try {
            if (settings.isEmpty()) {
                this.logger.error("Config JSON is not present. Returning defaultValue: [" + defaultValue + "].");
                return defaultValue;
            }

            Setting setting = settings.get(key);
            if (setting == null) {
                this.logger.error("Value not found for key " + key + ". Here are the available keys: " + String.join(", ", settings.keySet()));
                return defaultValue;
            }

            return (T) this.parseObject(classOfT, this.rolloutEvaluator.evaluate(setting, key, getEvaluateUser(user)).getKey());
        } catch (Exception e) {
            this.logger.error("Evaluating getValue('" + key + "') failed. Returning defaultValue: [" + defaultValue + "]. "
                    + e.getMessage(), e);
            return defaultValue;
        }
    }

    private String getVariationIdFromSettingsMap(Map<String, Setting> settings, String key, User user, String defaultVariationId) {
        try {
            if (settings.isEmpty()) {
                this.logger.error("Config JSON is not present. Returning defaultVariationId: [" + defaultVariationId + "].");
                return defaultVariationId;
            }

            Setting setting = settings.get(key);
            if (setting == null) {
                this.logger.error("Variation ID not found for key " + key + ". Here are the available keys: " + String.join(", ", settings.keySet()));
                return defaultVariationId;
            }
            return this.rolloutEvaluator.evaluate(setting, key, getEvaluateUser(user)).getValue();
        } catch (Exception e) {
            this.logger.error("Evaluating getVariationId('" + key + "') failed. Returning defaultVariationId: [" + defaultVariationId + "]. "
                    + e.getMessage(), e);
            return defaultVariationId;
        }
    }

    private <T> Map.Entry<String, T> getKeyAndValueFromSettingsMap(Class<T> classOfT, Map<String, Setting> settings, String variationId) {
        try {
            if (settings.isEmpty()) {
                this.logger.error("Config JSON is not present. Returning null.");
                return null;
            }

            for (Map.Entry<String, Setting> node : settings.entrySet()) {
                String settingKey = node.getKey();
                Setting setting = node.getValue();
                if (variationId.equals(setting.variationId)) {
                    return new AbstractMap.SimpleEntry<>(settingKey, (T) this.parseObject(classOfT, setting.value));
                }

                for (RolloutRule rolloutRule : setting.rolloutRules) {
                    if (variationId.equals(rolloutRule.variationId)) {
                        return new AbstractMap.SimpleEntry<>(settingKey, (T) this.parseObject(classOfT, rolloutRule.value));
                    }
                }

                for (RolloutPercentageItem percentageRule : setting.percentageItems) {
                    if (variationId.equals(percentageRule.variationId)) {
                        return new AbstractMap.SimpleEntry<>(settingKey, (T) this.parseObject(classOfT, percentageRule.value));
                    }
                }
            }

            return null;
        } catch (Exception e) {
            this.logger.error("Could not find the setting for the given variation ID: " + variationId);
            return null;
        }
    }

    private RefreshPolicyBase selectPolicy(PollingMode mode, ConfigFetcher fetcher, ConfigCatLogger logger, ConfigJsonCache configJsonCache) {
        if (mode instanceof AutoPollingMode) {
            return new AutoPollingPolicy(fetcher, logger, configJsonCache, (AutoPollingMode) mode);
        } else if (mode instanceof LazyLoadingMode) {
            return new LazyLoadingPolicy(fetcher, logger, configJsonCache, (LazyLoadingMode) mode);
        } else if (mode instanceof ManualPollingMode) {
            return new ManualPollingPolicy(fetcher, logger, configJsonCache);
        } else {
            throw new InvalidParameterException("The polling mode parameter is invalid.");
        }
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

    private Class<?> classBySettingType(int settingType) {
        if (settingType == SettingType.BOOLEAN.ordinal())
            return boolean.class;
        else if (settingType == SettingType.STRING.ordinal())
            return String.class;
        else if (settingType == SettingType.INT.ordinal())
            return int.class;
        else if (settingType == SettingType.DOUBLE.ordinal())
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
     * Get a singleton ConfigCat Client for the sdk key with the options, create a new client if not existed yet.
     * If the client already exist the options are ignored.
     *
     * @param sdkKey  the client sdk key.
     * @param options the client initializer options.
     * @return a singleton client.
     */
    public static ConfigCatClient get(final String sdkKey, final Options options) {
        if (sdkKey == null || sdkKey.isEmpty()) {
            throw new IllegalArgumentException("'sdkKey' cannot be null or empty.");
        }

        synchronized (INSTANCES) {
            Options clientOptions = options;

            ConfigCatClient client = INSTANCES.get(sdkKey);
            if (client != null) {
                if (clientOptions != null) {
                    client.logger.warn("Client for '" + sdkKey + "' is already created and will be reused; options passed are being ignored.");
                }
                return client;
            }

            if (clientOptions == null) {
                clientOptions = new Options();
            }
            client = new ConfigCatClient(sdkKey, clientOptions);
            INSTANCES.put(sdkKey, client);

            return client;
        }
    }

    /**
     * Options for configuring  {@link ConfigCatClient} instance.
     */
    public static class Options {
        private OkHttpClient httpClient;
        private ConfigCache cache = new NullConfigCache();
        private String baseUrl;
        private PollingMode pollingMode = PollingModes.autoPoll(60);
        private LogLevel logLevel = LogLevel.WARNING;
        private DataGovernance dataGovernance = DataGovernance.GLOBAL;
        private OverrideDataSourceBuilder localDataSourceBuilder;
        private OverrideBehaviour overrideBehaviour;
        private User defaultUser;

        /**
         * Sets the underlying http client which will be used to fetch the latest configuration.
         *
         * @param httpClient the http client.
         * @return the options.
         */
        public Options httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Sets the internal cache implementation.
         *
         * @param cache a {@link ConfigCache} implementation used to cache the configuration.
         * @return the options.
         */
        public Options cache(ConfigCache cache) {
            this.cache = cache;
            return this;
        }

        /**
         * Sets the base ConfigCat CDN url.
         *
         * @param baseUrl the base ConfigCat CDN url.
         * @return the options.
         */
        public Options baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the internal refresh policy implementation.
         *
         * @param pollingMode the polling mode.
         * @return the options.
         */
        public Options mode(PollingMode pollingMode) {
            this.pollingMode = pollingMode;
            return this;
        }

        /**
         * Default: Global. Set this parameter to be in sync with the Data Governance preference on the Dashboard:
         * https://app.configcat.com/organization/data-governance (Only Organization Admins have access)
         *
         * @param dataGovernance the {@link DataGovernance} parameter.
         * @return the options.
         */
        public Options dataGovernance(DataGovernance dataGovernance) {
            this.dataGovernance = dataGovernance;
            return this;
        }

        /**
         * Default: Warning. Sets the internal log level.
         *
         * @param logLevel the {@link LogLevel} parameter.
         * @return the options.
         */
        public Options logLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Sets feature flag and setting overrides.
         *
         * @param dataSourceBuilder builder that describes the overrides' data source.
         * @param behaviour         the override behaviour. It can be used to set preference on whether the local
         *                          values should override the remote values, or use local values only when a remote
         *                          value doesn't exist, or use it for local only mode.
         * @return the options.
         * @throws IllegalArgumentException when the <tt>dataSourceBuilder</tt> or <tt>behaviour</tt> parameter is null.
         */
        public Options flagOverrides(OverrideDataSourceBuilder dataSourceBuilder, OverrideBehaviour behaviour) {
            if (dataSourceBuilder == null) {
                throw new IllegalArgumentException("'dataSourceBuilder' cannot be null or empty.");
            }

            if (behaviour == null) {
                throw new IllegalArgumentException("'behaviour' cannot be null.");
            }

            this.localDataSourceBuilder = dataSourceBuilder;
            this.overrideBehaviour = behaviour;
            return this;
        }

        /**
         * Sets the default user.
         *
         * @param defaultUser the default user.
         * @return the options.
         */
        public Options defaultUser(User defaultUser) {
            this.defaultUser = defaultUser;
            return this;
        }
    }

}