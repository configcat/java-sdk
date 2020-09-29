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
     * @param sdkKey the sdk key.
     */
    ManualPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, String sdkKey) {
        super(configFetcher, cache, sdkKey);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        return CompletableFuture.completedFuture(super.readConfigCache());
    }
}
