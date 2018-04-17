package com.configcat;

import java.util.concurrent.CompletableFuture;

/**
 * Describes a {@link RefreshPolicy} which fetches the latest configuration
 * over HTTP every time when a get is called on the {@link ConfigCatClient}.
 */
public class ManualPollingPolicy extends RefreshPolicy {
       /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache the internal cache instance.
     */
    public ManualPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache) {
        super(configFetcher, cache);
        super.fetcher().setMode("m");
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        return super.fetcher().getConfigurationJsonStringAsync()
                .thenApply(response -> {
                    String cached = super.cache().get();
                    String config = response.config();
                    if (response.isFetched() && !config.equals(cached)) {
                        super.cache().set(response.config());
                    }

                    return response.isFetched() ? config : cached;
                });
    }
}
