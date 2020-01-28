package com.configcat;

import java.util.concurrent.CompletableFuture;

/**
 * Describes a {@link RefreshPolicy} which fetches the latest configuration
 * over HTTP every time when a get is called on the {@link ConfigCatClient}.
 */
class ManualPollingPolicy extends RefreshPolicy {
       /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache the internal cache instance.
     */
    ManualPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache) {
        super(configFetcher, cache);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        return CompletableFuture.completedFuture(super.cache().get());
    }
}
