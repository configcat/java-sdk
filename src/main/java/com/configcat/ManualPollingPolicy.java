package com.configcat;

import java.util.concurrent.CompletableFuture;

class ManualPollingPolicy extends RefreshPolicyBase {
    ManualPollingPolicy(ConfigFetcher configFetcher, ConfigCatLogger logger, ConfigJsonCache configJsonCache) {
        super(configFetcher, logger, configJsonCache);
    }

    @Override
    protected CompletableFuture<Config> getConfigurationAsync() {
        return CompletableFuture.completedFuture(super.configJsonCache.readFromCache());
    }
}
