package com.configcat;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

interface RefreshPolicy extends Closeable {
    CompletableFuture<Map<String, Setting>> getSettingsAsync();
    CompletableFuture<Void> refreshAsync();
}

abstract class RefreshPolicyBase implements RefreshPolicy {
    private final ConfigFetcher configFetcher;
    protected final ConfigJsonCache configJsonCache;
    protected final ConfigCatLogger logger;


    protected ConfigFetcher fetcher() {
        return configFetcher;
    }

    RefreshPolicyBase(ConfigFetcher configFetcher, ConfigCatLogger logger, ConfigJsonCache configJsonCache) {
        this.configFetcher = configFetcher;
        this.logger = logger;
        this.configJsonCache = configJsonCache;
    }

    public CompletableFuture<Map<String, Setting>> getSettingsAsync() {
        return this.getConfigurationAsync()
                .thenApply(config -> config.entries);
    }

    protected abstract CompletableFuture<Config> getConfigurationAsync();

    public CompletableFuture<Void> refreshAsync() {
        return this.fetcher().fetchAsync()
                .thenAcceptAsync(response -> {
                    if (response.isFetched()) {
                        this.configJsonCache.writeToCache(response.config());
                    }
                });
    }

    @Override
    public void close() throws IOException {
        this.configFetcher.close();
    }
}

class NullRefreshPolicy implements RefreshPolicy {
    @Override
    public CompletableFuture<Map<String, Setting>> getSettingsAsync() {
        return CompletableFuture.completedFuture(new HashMap<>());
    }

    @Override
    public CompletableFuture<Void> refreshAsync() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() throws IOException { }
}
