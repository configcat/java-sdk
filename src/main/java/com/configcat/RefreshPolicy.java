package com.configcat;

import org.apache.commons.codec.digest.MurmurHash3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * The public interface of a refresh policy which's implementors
 * should describe the configuration update rules.
 */
abstract class RefreshPolicy implements Closeable {
    protected static final Logger LOGGER = LoggerFactory.getLogger(RefreshPolicy.class);
    private static final String CacheBase = "config-v5-%s";
    private final ConfigCache cache;
    private final ConfigFetcher configFetcher;
    private final String CacheKey;

    private String inMemoryConfig;

    protected String readConfigCache() {
        try {
            return this.cache.read(CacheKey);
        } catch (Exception e) {
            LOGGER.error("An error occurred during the cache read.", e);
            return this.inMemoryConfig;
        }
    }

    protected void writeConfigCache(String value) {
        try {
            this.inMemoryConfig = value;
            this.cache.write(CacheKey, value);
        } catch (Exception e) {
            LOGGER.error("An error occurred during the cache write.", e);
        }
    }

    /**
     * Through this getter, child classes can use the fetcher to
     * get the latest configuration over HTTP.
     *
     * @return the config fetcher.
     */
    protected ConfigFetcher fetcher() {
        return configFetcher;
    }

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache the internal cache instance.
     * @param sdkKey the sdk key.
     */
    RefreshPolicy(ConfigFetcher configFetcher, ConfigCache cache, String sdkKey) {
        this.configFetcher = configFetcher;
        this.cache = cache;
        this.CacheKey = String.format(CacheBase, Integer.toString(MurmurHash3.hash32(sdkKey.getBytes()), 32));
    }

    /**
     * Child classes has to implement this method, the {@link ConfigCatClient}
     * uses it to read the current configuration value through the applied policy.
     *
     * @return the future which computes the configuration.
     */
    public abstract CompletableFuture<String> getConfigurationJsonAsync();

    /**
     * Initiates a force refresh on the cached configuration.
     *
     * @return the future which executes the refresh.
     */
    public CompletableFuture<Void> refreshAsync() {
        return this.fetcher().getConfigurationJsonStringAsync()
                .thenAcceptAsync(response -> {
                    if(response.isFetched())
                        this.writeConfigCache(response.config());
                });
    }

    String getLatestCachedValue() {
        return this.inMemoryConfig;
    }

    @Override
    public void close() throws IOException {
        this.configFetcher.close();
    }
}
