package com.configcat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes a {@link RefreshPolicy} which uses an expiring cache
 * to maintain the internally stored configuration.
 */
class LazyLoadingPolicy extends RefreshPolicy {
    private Instant lastRefreshedTime;
    private final int cacheRefreshIntervalInSeconds;
    private final boolean asyncRefresh;
    private final AtomicBoolean isFetching;
    private final AtomicBoolean initialized;
    private CompletableFuture<String> fetchingFuture;
    private final CompletableFuture<Void> init;

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache the internal cache instance.
     * @param sdkKey the sdk key.
     * @param config the polling mode configuration.
     */
    LazyLoadingPolicy(ConfigFetcher configFetcher, ConfigCache cache, Logger logger, String sdkKey, LazyLoadingMode config) {
        super(configFetcher, cache, logger, sdkKey);
        this.asyncRefresh = config.isAsyncRefresh();
        this.cacheRefreshIntervalInSeconds = config.getCacheRefreshIntervalInSeconds();
        this.isFetching = new AtomicBoolean(false);
        this.initialized = new AtomicBoolean(false);
        this.lastRefreshedTime = Instant.MIN;
        this.init = new CompletableFuture<>();
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        if(Instant.now().isAfter(lastRefreshedTime.plusSeconds(this.cacheRefreshIntervalInSeconds))) {
            boolean isInitialized = this.init.isDone();

            if(isInitialized && !this.isFetching.compareAndSet(false, true))
                return this.asyncRefresh && this.initialized.get()
                        ? CompletableFuture.completedFuture(super.readConfigCache())
                        : this.fetchingFuture;

            logger.debug("Cache expired, refreshing.");
            if(isInitialized) {
                this.fetchingFuture = this.fetch();
                if(this.asyncRefresh) {
                    return CompletableFuture.completedFuture(super.readConfigCache());
                }
                return this.fetchingFuture;
            } else {
                if(this.isFetching.compareAndSet(false, true)) {
                    this.fetchingFuture = this.fetch();
                }
                return this.init.thenApplyAsync(v -> super.readConfigCache());
            }
        }

        return CompletableFuture.completedFuture(super.readConfigCache());
    }

    private CompletableFuture<String> fetch() {
        return super.fetcher().getConfigurationJsonStringAsync()
                .thenApplyAsync(response -> {
                    String cached = super.readConfigCache();
                    if (response.isFetched() && !response.config().equals(cached)) {
                        super.writeConfigCache(response.config());
                    }

                    if(!response.isFailed())
                        this.lastRefreshedTime = Instant.now();

                    if(this.initialized.compareAndSet(false, true)) {
                        this.init.complete(null);
                    }

                    this.isFetching.set(false);

                    return response.isFetched() ? response.config() : cached;
                });
    }
}
