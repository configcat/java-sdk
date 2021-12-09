package com.configcat;

import java.util.concurrent.CompletableFuture;

class ManualPollingPolicy extends DefaultRefreshPolicy {
    ManualPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, ConfigCatLogger logger, ConfigMemoryCache configMemoryCache, String sdkKey) {
        super(configFetcher, cache, logger, configMemoryCache, sdkKey);
    }

    @Override
    public CompletableFuture<Config> getConfigurationAsync() {
        return CompletableFuture.completedFuture(super.readConfigCache());
    }
}
