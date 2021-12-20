package com.configcat;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes a {@link RefreshPolicyBase} which uses an expiring cache
 * to maintain the internally stored configuration.
 */
class LazyLoadingPolicy extends RefreshPolicyBase {
    private Instant lastRefreshedTime;
    private final int cacheRefreshIntervalInSeconds;
    private final boolean asyncRefresh;
    private final AtomicBoolean isFetching;
    private final AtomicBoolean initialized;
    private CompletableFuture<Config> fetchingFuture;
    private final CompletableFuture<Void> init;

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache         the internal cache instance.
     * @param sdkKey        the sdk key.
     * @param config        the polling mode configuration.
     */
    LazyLoadingPolicy(ConfigFetcher configFetcher, ConfigCache cache, ConfigCatLogger logger, ConfigMemoryCache configMemoryCache, String sdkKey, LazyLoadingMode config) {
        super(configFetcher, cache, logger, configMemoryCache, sdkKey);
        this.asyncRefresh = config.isAsyncRefresh();
        this.cacheRefreshIntervalInSeconds = config.getCacheRefreshIntervalInSeconds();
        this.isFetching = new AtomicBoolean(false);
        this.initialized = new AtomicBoolean(false);
        this.lastRefreshedTime = Instant.MIN;
        this.init = new CompletableFuture<>();
    }

    @Override
    protected CompletableFuture<Config> getConfigurationAsync() {
        if (Instant.now().isAfter(lastRefreshedTime.plusSeconds(this.cacheRefreshIntervalInSeconds))) {
            boolean isInitialized = this.init.isDone();

            if (isInitialized && !this.isFetching.compareAndSet(false, true))
                return this.asyncRefresh && this.initialized.get()
                        ? CompletableFuture.completedFuture(super.readConfigCache())
                        : this.fetchingFuture;

            logger.debug("Cache expired, refreshing.");
            if (isInitialized) {
                this.fetchingFuture = this.fetch();
                if (this.asyncRefresh) {
                    return CompletableFuture.completedFuture(super.readConfigCache());
                }
                return this.fetchingFuture;
            } else {
                if (this.isFetching.compareAndSet(false, true)) {
                    this.fetchingFuture = this.fetch();
                }
                return this.init.thenApplyAsync(v -> super.readConfigCache());
            }
        }

        return CompletableFuture.completedFuture(super.readConfigCache());
    }

    private CompletableFuture<Config> fetch() {
        return super.fetcher().getConfigurationAsync()
                .thenApplyAsync(response -> {
                    Config cachedConfig = super.readConfigCache();
                    Config fetchedConfig = response.config();
                    if (response.isFetched() && !fetchedConfig.equals(cachedConfig)) {
                        super.writeConfigCache(fetchedConfig);
                    }

                    if (!response.isFailed())
                        this.lastRefreshedTime = Instant.now();

                    if (this.initialized.compareAndSet(false, true)) {
                        this.init.complete(null);
                    }

                    this.isFetching.set(false);

                    return response.isFetched() ? fetchedConfig : cachedConfig;
                });
    }
}
