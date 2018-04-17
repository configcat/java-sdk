package com.configcat;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes a {@link RefreshPolicy} which uses an expiring cache
 * to maintain the internally stored configuration.
 */
public class ExpiringCachePolicy extends RefreshPolicy {
    private Instant lastRefreshedTime;
    private int cacheRefreshIntervalInSeconds;
    private boolean asyncRefresh;
    private final AtomicBoolean isFetching;
    private final AtomicBoolean initialized;
    private CompletableFuture<String> fetchingFuture;

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache the internal cache instance.
     */
    private ExpiringCachePolicy(ConfigFetcher configFetcher, ConfigCache cache, Builder builder) {
        super(configFetcher, cache);
        super.fetcher().setMode("l");
        this.asyncRefresh = builder.asyncRefresh;
        this.cacheRefreshIntervalInSeconds = builder.cacheRefreshIntervalInSeconds;
        this.isFetching = new AtomicBoolean(false);
        this.initialized = new AtomicBoolean(false);
        this.lastRefreshedTime = Instant.MIN;
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        if(Instant.now().isAfter(lastRefreshedTime.plusSeconds(this.cacheRefreshIntervalInSeconds))) {
            if(!this.isFetching.compareAndSet(false, true))
                return this.asyncRefresh && this.initialized.get()
                        ? CompletableFuture.completedFuture(super.cache().get())
                        : this.fetchingFuture;


            this.fetchingFuture = super.fetcher().getConfigurationJsonStringAsync()
                    .thenApplyAsync(response -> {
                        String cached = super.cache().get();
                        if (response.isFetched() && !response.config().equals(cached)) {
                            super.cache().set(response.config());
                            this.isFetching.set(false);
                            this.initialized.set(true);
                        }

                        if(!response.isFailed())
                            this.lastRefreshedTime = Instant.now();

                        return response.isFetched() ? response.config() : cached;
                    });

            return this.asyncRefresh && this.initialized.get()
                    ? CompletableFuture.completedFuture(super.cache().get())
                    : this.fetchingFuture;
        }

        return CompletableFuture.completedFuture(super.cache().get());
    }

    /**
     * Creates a new builder instance.
     *
     * @return the new builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder that helps construct a {@link ExpiringCachePolicy} instance.
     */
    public static class Builder {
        private int cacheRefreshIntervalInSeconds = 60;
        private boolean asyncRefresh;

        /**
         * Sets how long the cache will store its value before fetching the
         * latest from the network again.
         *
         * @param cacheRefreshIntervalInSeconds the refresh interval value in seconds.
         * @return the builder.
         */
        public Builder cacheRefreshIntervalInSeconds(int cacheRefreshIntervalInSeconds) {
            this.cacheRefreshIntervalInSeconds = cacheRefreshIntervalInSeconds;
            return this;
        }

        /**
         * Sets whether the cache should refresh itself asynchronously or synchronously.
         * <p>If it's set to {@code true} reading from the policy will not wait for the refresh to be finished,
         * instead it returns immediately with the previous stored value.</p>
         * <p>If it's set to {@code false} the policy will wait until the expired
         * value is being refreshed with the latest configuration.</p>
         *
         * @param asyncRefresh the refresh behavior.
         * @return the builder.
         */
        public Builder asyncRefresh(boolean asyncRefresh) {
            this.asyncRefresh = asyncRefresh;
            return this;
        }

        /**
         * Builds the configured {@link ExpiringCachePolicy} instance.
         *
         * @param configFetcher the internal config fetcher.
         * @param cache the internal cache.
         * @return the configured {@link ExpiringCachePolicy} instance
         */
        public ExpiringCachePolicy build(ConfigFetcher configFetcher, ConfigCache cache) {
            return new ExpiringCachePolicy(configFetcher, cache, this);
        }
    }
}
