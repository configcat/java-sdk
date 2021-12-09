package com.configcat;

import java.util.concurrent.CompletableFuture;

class ManualPollingPolicy extends DefaultRefreshPolicy {
    ManualPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, ConfigCatLogger logger, ConfigMemoryCache deserializer, String sdkKey) {
        super(configFetcher, cache, logger, deserializer, sdkKey);
    }

    @Override
    public CompletableFuture<Config> getConfigurationAsync() {
        return CompletableFuture.completedFuture(super.readConfigCache());
    }
}
