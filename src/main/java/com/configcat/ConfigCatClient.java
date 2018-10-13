package com.configcat;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * A client for handling configurations provided by ConfigCat.
 */
public class ConfigCatClient implements ConfigurationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigCatClient.class);
    private static final ConfigurationParser parser = new ConfigurationParser();
    private final RefreshPolicy refreshPolicy;
    private final int maxWaitTimeForSyncCallsInSeconds;

    private ConfigCatClient(String apiKey, Builder builder) throws IllegalArgumentException {
        if(apiKey == null || apiKey.isEmpty())
            throw new IllegalArgumentException("apiKey is null or empty");

        this.maxWaitTimeForSyncCallsInSeconds = builder.maxWaitTimeForSyncCallsInSeconds;

        ConfigFetcher fetcher = new ConfigFetcher(builder.httpClient == null
                ? new OkHttpClient
                    .Builder()
                    .retryOnConnectionFailure(true)
                    .build()
                : builder.httpClient, apiKey);

        ConfigCache cache = builder.cache == null
                ? new InMemoryConfigCache()
                : builder.cache;

        this.refreshPolicy = builder.refreshPolicy == null
                ? AutoPollingPolicy.newBuilder()
                    .build(fetcher, cache)
                : builder.refreshPolicy.apply(fetcher, cache);
    }

    /**
     * Constructs a new client instance with the default configuration.
     *
     * @param apiKey the token which identifies your project configuration.
     */
    public ConfigCatClient(String apiKey) {
        this(apiKey, newBuilder());
    }

    @Override
    public <T extends RefreshPolicy> T getRefreshPolicy(Class<T> classOfT) {
        return classOfT.cast(this.refreshPolicy);
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
            LOGGER.error("An error occurred during the reading of the value for key '"+key+"'.", e);
            return this.getDefaultJsonValue(classOfT, key, user, defaultValue);
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
                .thenApply(config -> {
                    try {
                        return parser.parseValue(classOfT, config, key, user);
                    } catch (Exception e) {
                        LOGGER.error("An error occurred during the deserialization of the value for key '"+key+"'.", e);
                        return this.getDefaultJsonValue(classOfT, key, user, defaultValue);
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

    private <T> T getJsonValue(Class<T> classOfT, String config, String key, User user, T defaultValue) {
        try {
            return parser.parseValue(classOfT, config, key, user);
        } catch (Exception e) {
            LOGGER.error("An error occurred during the deserialization of the value for key '"+key+"'.", e);
            return defaultValue;
        }
    }

    private <T> T getDefaultJsonValue(Class<T> classOfT, String key, User user, T defaultValue) {
        String latest = this.refreshPolicy.getLatestCachedValue();
        return latest != null ? this.getJsonValue(classOfT, latest, key, user, defaultValue) : defaultValue;
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
        private BiFunction<ConfigFetcher, ConfigCache, RefreshPolicy> refreshPolicy;

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
         * Sets the internal refresh policy implementation.
         *
         * @param refreshPolicy a function used to create the a {@link RefreshPolicy} implementation with the given {@link ConfigFetcher} and {@link ConfigCache}.
         * @return the builder.
         */
        public Builder refreshPolicy(BiFunction<ConfigFetcher, ConfigCache, RefreshPolicy> refreshPolicy) {
            this.refreshPolicy = refreshPolicy;
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
         * @param apiKey the project token.
         * @return the configured {@link ConfigCatClient} instance.
         */
        public ConfigCatClient build(String apiKey) {
            return new ConfigCatClient(apiKey, this);
        }
    }
}