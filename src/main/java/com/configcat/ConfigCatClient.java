package com.configcat;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * A client for handling configurations provided by ConfigCat.
 */
public final class ConfigCatClient implements ConfigurationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigCatClient.class);
    private static final ConfigurationParser parser = new ConfigurationParser();
    private static final String BASE_URL_GLOBAL = "https://cdn-global.configcat.com";
    private static final String BASE_URL_EU = "https://cdn-eu.configcat.com";

    private final RefreshPolicy refreshPolicy;
    private final int maxWaitTimeForSyncCallsInSeconds;

    private ConfigCatClient(String sdkKey, Builder builder) throws IllegalArgumentException {
        if(sdkKey == null || sdkKey.isEmpty())
            throw new IllegalArgumentException("sdkKey is null or empty");

        this.maxWaitTimeForSyncCallsInSeconds = builder.maxWaitTimeForSyncCallsInSeconds;

        PollingMode pollingMode = builder.pollingMode == null
                ? PollingModes.AutoPoll(60)
                : builder.pollingMode;

        boolean hasCustomBaseUrl = builder.baseUrl != null && !builder.baseUrl.isEmpty();
        ConfigFetcher fetcher = new ConfigFetcher(builder.httpClient == null
                ? new OkHttpClient
                    .Builder()
                    .retryOnConnectionFailure(true)
                    .build()
                : builder.httpClient,
                sdkKey,
                !hasCustomBaseUrl
                    ? builder.dataGovernance == DataGovernance.GLOBAL
                        ? BASE_URL_GLOBAL
                        : BASE_URL_EU
                    : builder.baseUrl,
                hasCustomBaseUrl,
                pollingMode.getPollingIdentifier());

        ConfigCache cache = builder.cache == null
                ? new InMemoryConfigCache()
                : builder.cache;

        this.refreshPolicy = pollingMode.accept(new RefreshPolicyFactory(cache, fetcher, sdkKey));
    }

    /**
     * Constructs a new client instance with the default configuration.
     *
     * @param sdkKey the token which identifies your project configuration.
     */
    public ConfigCatClient(String sdkKey) {
        this(sdkKey, newBuilder());
    }

    @Override
    public  <T> T getValue(Class<T> classOfT, String key, T defaultValue) {
        return this.getValue(classOfT, key, null, defaultValue);
    }

    @Override
    public  <T> T getValue(Class<T> classOfT, String key, User user, T defaultValue) {
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        if(classOfT != String.class &&
                classOfT != Integer.class &&
                classOfT != int.class &&
                classOfT != Double.class &&
                classOfT != double.class &&
                classOfT != Boolean.class &&
                classOfT != boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");

        try {
            return this.maxWaitTimeForSyncCallsInSeconds > 0
                    ? this.getValueAsync(classOfT, key, user, defaultValue).get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS)
                    : this.getValueAsync(classOfT, key, user, defaultValue).get();
        } catch (Exception e) {
            return this.getValueFromJson(classOfT, this.refreshPolicy.getLatestCachedValue(), key, user, defaultValue);
        }
    }

    @Override
    public <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, T defaultValue) {
        return this.getValueAsync(classOfT, key, null, defaultValue);
    }

    @Override
    public <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, User user, T defaultValue) {
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        if(classOfT != String.class &&
                classOfT != Integer.class &&
                classOfT != int.class &&
                classOfT != Double.class &&
                classOfT != double.class &&
                classOfT != Boolean.class &&
                classOfT != boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");

        return this.refreshPolicy.getConfigurationJsonAsync()
                .thenApply(config -> this.getValueFromJson(classOfT, config, key, user, defaultValue));
    }

    @Override
    public String getVariationId(String key, String defaultVariationId) {
        return this.getVariationId(key, null, defaultVariationId);
    }

    @Override
    public String getVariationId(String key, User user, String defaultVariationId) {
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        try {
            return this.maxWaitTimeForSyncCallsInSeconds > 0
                    ? this.getVariationIdAsync(key, user, defaultVariationId).get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS)
                    : this.getVariationIdAsync(key, user, defaultVariationId).get();
        } catch (Exception e) {
            return this.getVariationIdFromJson(this.refreshPolicy.getLatestCachedValue(), key, user, defaultVariationId);
        }
    }

    @Override
    public CompletableFuture<String> getVariationIdAsync(String key, String defaultVariationId) {
        return this.getVariationIdAsync(key, null, defaultVariationId);
    }

    @Override
    public CompletableFuture<String> getVariationIdAsync(String key, User user, String defaultVariationId) {
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        return this.refreshPolicy.getConfigurationJsonAsync()
                .thenApply(config -> this.getVariationIdFromJson(config, key, user, defaultVariationId));
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
            return this.maxWaitTimeForSyncCallsInSeconds > 0
                    ? this.getAllVariationIdsAsync(user).get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS)
                    : this.getAllVariationIdsAsync(user).get();
        } catch (Exception e) {
            LOGGER.error("An error occurred during getting all the variation ids.", e);
            return new ArrayList<>();
        }
    }

    @Override
    public CompletableFuture<Collection<String>> getAllVariationIdsAsync(User user) {
        return this.refreshPolicy.getConfigurationJsonAsync()
                .thenApply(config -> {
                    try {
                        Collection<String> keys = parser.getAllKeys(config);
                        ArrayList<String> result = new ArrayList<>();

                        for (String key : keys) {
                            result.add(this.getVariationIdFromJson(config, key, user, null));
                        }

                        return result;
                    } catch (Exception e) {
                        LOGGER.error("An error occurred during the deserialization. Returning empty array.", e);
                        return new ArrayList<>();
                    }
                });
    }

    @Override
    public <T> Map.Entry<String, T> getKeyAndValue(Class<T> classOfT, String variationId) {
        if(variationId == null || variationId.isEmpty())
            throw new IllegalArgumentException("variationId is null or empty");

        try {
            return this.maxWaitTimeForSyncCallsInSeconds > 0
                    ? this.getKeyAndValueAsync(classOfT, variationId).get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS)
                    : this.getKeyAndValueAsync(classOfT, variationId).get();
        } catch (Exception e) {
            return this.getKeyAndValueFromJson(classOfT, this.refreshPolicy.getLatestCachedValue(), variationId);
        }
    }

    @Override
    public <T> CompletableFuture<Map.Entry<String, T>> getKeyAndValueAsync(Class<T> classOfT, String variationId) {
        if(variationId == null || variationId.isEmpty())
            throw new IllegalArgumentException("variationId is null or empty");

        return this.refreshPolicy.getConfigurationJsonAsync()
                .thenApply(config -> this.getKeyAndValueFromJson(classOfT, config, variationId));
    }

    @Override
    public Collection<String> getAllKeys() {
        try {
            return this.maxWaitTimeForSyncCallsInSeconds > 0
                    ? this.getAllKeysAsync().get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS)
                    : this.getAllKeysAsync().get();
        } catch (Exception e) {
            LOGGER.error("An error occurred during getting all the setting keys.", e);
            return new ArrayList<>();
        }
    }

    @Override
    public CompletableFuture<Collection<String>> getAllKeysAsync() {
        return this.refreshPolicy.getConfigurationJsonAsync()
                .thenApply(config -> {
                   try {
                       return parser.getAllKeys(config);
                   } catch (Exception e) {
                       LOGGER.error("An error occurred during the deserialization. Returning empty array.", e);
                       return new ArrayList<>();
                   }
                });
    }

    @Override
    public void forceRefresh() {
        try {
            if(this.maxWaitTimeForSyncCallsInSeconds > 0)
                this.forceRefreshAsync().get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS);
            else
                this.forceRefreshAsync().get();
        } catch (Exception e) {
            LOGGER.error("An error occurred during the refresh.", e);
        }
    }

    @Override
    public CompletableFuture<Void> forceRefreshAsync() {
        return this.refreshPolicy.refreshAsync();
    }

    @Override
    public void close() throws IOException {
        this.refreshPolicy.close();
    }

    private <T> T getValueFromJson(Class<T> classOfT, String config, String key, User user, T defaultValue) {
        try {
            return parser.parseValue(classOfT, config, key, user);
        } catch (Exception e) {
            LOGGER.error("Evaluating getValue('"+key+"') failed. Returning defaultValue: ["+ defaultValue +"]. "
                    + e.getMessage(), e);
            return defaultValue;
        }
    }

    private String getVariationIdFromJson(String config, String key, User user, String defaultVariationId) {
        try {
            return parser.parseVariationId(config, key, user);
        } catch (Exception e) {
            LOGGER.error("Evaluating getVariationId('"+key+"') failed. Returning defaultVariationId: ["+ defaultVariationId +"]. "
                    + e.getMessage(), e);
            return defaultVariationId;
        }
    }

    private <T> Map.Entry<String, T> getKeyAndValueFromJson(Class<T> classOfT, String config, String variationId) {
        try {
            return parser.parseKeyValue(classOfT, config, variationId);
        } catch (Exception e) {
            LOGGER.error("Could not find the setting for the given variation ID: " + variationId);
            return null;
        }
    }

    /**
     * Creates a new builder instance.
     *
     * @return the new builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder that helps construct a {@link ConfigCatClient} instance.
     */
    public static class Builder {
        private OkHttpClient httpClient;
        private ConfigCache cache;
        private int maxWaitTimeForSyncCallsInSeconds;
        private String baseUrl;
        private PollingMode pollingMode;
        private DataGovernance dataGovernance = DataGovernance.GLOBAL;

        /**
         * Sets the underlying http client which will be used to fetch the latest configuration.
         *
         * @param httpClient the http client.
         * @return the builder.
         */
        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Sets the internal cache implementation.
         *
         * @param cache a {@link ConfigFetcher} implementation used to cache the configuration.
         * @return the builder.
         */
        public Builder cache(ConfigCache cache) {
            this.cache = cache;
            return this;
        }

        /**
         * Sets the base ConfigCat CDN url.
         *
         * @param baseUrl the base ConfigCat CDN url.
         * @return the builder.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the internal refresh policy implementation.
         *
         * @param pollingMode the polling mode.
         * @return the builder.
         */
        public Builder mode(PollingMode pollingMode) {
            this.pollingMode = pollingMode;
            return this;
        }

        /**
         * Set this parameter to restrict the location of your feature flag and setting data within the ConfigCat CDN.
         * This parameter must be in sync with the preferences on: https://app.configcat.com/organization/data-governance
         * (Only Organization Admins can set this preference.)
         *
         * @param dataGovernance the {@link DataGovernance} parameter.
         * @return the builder.
         */
        public Builder dataGovernance(DataGovernance dataGovernance) {
            this.dataGovernance = dataGovernance;
            return this;
        }

        /**
         * Sets the maximum time in seconds at most how long the synchronous calls
         * e.g. {@code client.getConfiguration(...)} have to be blocked.
         *
         * @param maxWaitTimeForSyncCallsInSeconds the maximum time in seconds at most how long the synchronous calls
         *                                        e.g. {@code client.getConfiguration(...)} have to be blocked.
         * @return the builder.
         * @throws IllegalArgumentException when the given value is lesser than 2.
         */
        public Builder maxWaitTimeForSyncCallsInSeconds(int maxWaitTimeForSyncCallsInSeconds) {
            if(maxWaitTimeForSyncCallsInSeconds < 2)
                throw new IllegalArgumentException("maxWaitTimeForSyncCallsInSeconds cannot be less than 2 seconds");

            this.maxWaitTimeForSyncCallsInSeconds = maxWaitTimeForSyncCallsInSeconds;
            return this;
        }

        /**
         * Builds the configured {@link ConfigCatClient} instance.
         *
         * @param sdkKey the project token.
         * @return the configured {@link ConfigCatClient} instance.
         */
        public ConfigCatClient build(String sdkKey) {
            return new ConfigCatClient(sdkKey, this);
        }
    }
}