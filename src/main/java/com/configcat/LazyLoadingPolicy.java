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
public class LazyLoadingPolicy extends RefreshPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger(LazyLoadingPolicy.class);
    private Instant lastRefreshedTime;
    private int cacheRefreshIntervalInSeconds;
    private boolean asyncRefresh;
    private final AtomicBoolean isFetching;
    private final AtomicBoolean initialized;
    private CompletableFuture<String> fetchingFuture;
    private CompletableFuture<Void> init;

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache the internal cache instance.
     */
    private LazyLoadingPolicy(ConfigFetcher configFetcher, ConfigCache cache, Builder builder) {
        super(configFetcher, cache);
        super.fetcher().setMode("l");
        this.asyncRefresh = builder.asyncRefresh;
        this.cacheRefreshIntervalInSeconds = builder.cacheRefreshIntervalInSeconds;
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
                        ? CompletableFuture.completedFuture(super.cache().get())
                        : this.fetchingFuture;

            LOGGER.debug("Cache expired, refreshing");
            if(isInitialized) {
                this.fetchingFuture = this.fetch();
                if(this.asyncRefresh) {
                    return CompletableFuture.completedFuture(super.cache().get());
                }
                return this.fetchingFuture;
            } else {
                if(this.isFetching.compareAndSet(false, true)) {
                    this.fetchingFuture = this.fetch();
                }
                return this.init.thenApplyAsync(v -> super.cache().get());
            }
        }

        return CompletableFuture.completedFuture(super.cache().get());
    }

    private CompletableFuture<String> fetch() {
        return super.fetcher().getConfigurationJsonStringAsync()
                .thenApplyAsync(response -> {
                    String cached = super.cache().get();
                    if (response.isFetched() && !response.config().equals(cached)) {
                        super.cache().set(response.config());
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

    /**
     * Creates a new builder instance.
     *
     * @return the new builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder that helps construct a {@link LazyLoadingPolicy} instance.
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
         * Builds the configured {@link LazyLoadingPolicy} instance.
         *
         * @param configFetcher the internal config fetcher.
         * @param cache the internal cache.
         * @return the configured {@link LazyLoadingPolicy} instance
         */
        public LazyLoadingPolicy build(ConfigFetcher configFetcher, ConfigCache cache) {
            return new LazyLoadingPolicy(configFetcher, cache, this);
        }
    }
}
