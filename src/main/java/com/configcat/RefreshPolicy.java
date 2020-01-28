package com.configcat;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * The public interface of a refresh policy which's implementors
 * should describe the configuration update rules.
 */
abstract class RefreshPolicy implements Closeable {
    private final ConfigCache cache;
    private final ConfigFetcher configFetcher;

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
     * Through this getter, child classes can use the cache to
     * control the cached configuration.
     *
     * @return the config cache.
     */
    protected ConfigCache cache() {
        return cache;
    }

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache the internal cache instance.
     */
    RefreshPolicy(ConfigFetcher configFetcher, ConfigCache cache) {
        this.configFetcher = configFetcher;
        this.cache = cache;
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
                        this.cache().set(response.config());
                });
    }

    String getLatestCachedValue() {
        return this.cache.inMemoryValue();
    }

    @Override
    public void close() throws IOException {
        this.configFetcher.close();
    }
}
