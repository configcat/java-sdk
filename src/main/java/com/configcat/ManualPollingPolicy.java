package com.configcat;

import org.slf4j.Logger;
import java.util.concurrent.CompletableFuture;

class ManualPollingPolicy extends RefreshPolicy {
    ManualPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, Logger logger, String sdkKey) {
        super(configFetcher, cache, logger, sdkKey);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        return CompletableFuture.completedFuture(super.readConfigCache());
    }
}
