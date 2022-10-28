package com.configcat;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigService implements Closeable {

    private final ConfigFetcher configFetcher;
    private final ConfigJsonCache configJsonCache;
    private final ConfigCatLogger logger;
    private final PollingMode pollingMode;

    //APP
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService initScheduler;
    private CompletableFuture<Void> initFuture;

    private ArrayList<ConfigurationChangeListener> listeners;
    //LPP  - initialized is common
    private Instant lastRefreshedTime;
    private int cacheRefreshIntervalInSeconds;
    private boolean asyncRefresh;
    private AtomicBoolean isFetching;
    private CompletableFuture<Config> fetchingFuture;
    private CompletableFuture<Void> init;
    //COMMON
    private AtomicBoolean initialized;
    private boolean offline;


    public ConfigService(String sdkKey, ConfigFetcher configFetcher, PollingMode pollingMode, ConfigCache configCache, ConfigCatLogger logger, boolean offline) {
        //TODO store sdkKey
        this.configFetcher = configFetcher;
        this.pollingMode = pollingMode;
        this.configJsonCache = new ConfigJsonCache(logger, configCache, sdkKey);
        this.logger = logger;
        this.offline = offline;
        //TODO init what we need. check on offline

        //TODO refactore PP inits
        if (pollingMode instanceof AutoPollingMode) {
            //TODO do what auto polling dose
            AutoPollingMode config = (AutoPollingMode) pollingMode;
            this.listeners = new ArrayList<>();

            if (config.getListener() != null)
                this.listeners.add(config.getListener());

            this.initialized = new AtomicBoolean(false);
            this.initFuture = new CompletableFuture<>();

            this.initScheduler = Executors.newSingleThreadScheduledExecutor();
            this.initScheduler.schedule(() -> {
                if (!this.initialized.getAndSet(true))
                    this.initFuture.complete(null);
            }, config.getMaxInitWaitTimeSeconds(), TimeUnit.SECONDS);

            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(() -> {
                try {
                    FetchResponse response = this.configFetcher.fetchAsync().get();
                    if (response.isFetched()) {
                        this.configJsonCache.writeToCache(response.config());
                        this.broadcastConfigurationChanged();
                    }

                    if (!this.initialized.getAndSet(true))
                        this.initFuture.complete(null);
                } catch (Exception e) {
                    logger.error("Exception in AutoPollingCachePolicy", e);
                }
            }, 0, config.getAutoPollRateInSeconds(), TimeUnit.SECONDS);

        } else if (pollingMode instanceof LazyLoadingMode) {
            LazyLoadingMode config = (LazyLoadingMode) pollingMode;
            this.asyncRefresh = config.isAsyncRefresh();
            this.cacheRefreshIntervalInSeconds = config.getCacheRefreshIntervalInSeconds();
            this.isFetching = new AtomicBoolean(false);
            this.initialized = new AtomicBoolean(false);
            this.lastRefreshedTime = Instant.MIN;
            this.init = new CompletableFuture<>();
        }
        // nothing to do if MPM
    }

    public CompletableFuture<Void> refreshAsync() {
        return this.configFetcher.fetchAsync()
                .thenAcceptAsync(response -> {
                    if (response.isFetched()) {
                        this.configJsonCache.writeToCache(response.config());
                    }
                });
    }

    public CompletableFuture<Map<String, Setting>> getSettingsAsync() {
        //TODO implement
        if (pollingMode instanceof AutoPollingMode) {
            if (this.initFuture.isDone())
                return CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(config -> config.entries);

            return this.initFuture.thenApplyAsync(v -> this.configJsonCache.readFromCache()).thenApply(config -> config.entries);
        }
        if (pollingMode instanceof LazyLoadingMode) {
            if (Instant.now().isAfter(lastRefreshedTime.plusSeconds(this.cacheRefreshIntervalInSeconds))) {
                boolean isInitialized = this.init.isDone();

                if (isInitialized && !this.isFetching.compareAndSet(false, true))
                    return this.asyncRefresh && this.initialized.get()
                            ? CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(config -> config.entries)
                            : this.fetchingFuture.thenApply(config -> config.entries);

                logger.debug("Cache expired, refreshing.");
                if (isInitialized) {
                    this.fetchingFuture = this.fetch();
                    if (this.asyncRefresh) {
                        return CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(config -> config.entries);
                    }
                    return this.fetchingFuture.thenApply(config -> config.entries);
                } else {
                    if (this.isFetching.compareAndSet(false, true)) {
                        this.fetchingFuture = this.fetch();
                    }
                    return this.init.thenApplyAsync(v -> this.configJsonCache.readFromCache()).thenApply(config -> config.entries);
                }
            }

            return CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(config -> config.entries);
        }
        //MPP the last
        return CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(config -> config.entries);
    }

    // LPP fetch
    private CompletableFuture<Config> fetch() {
        return this.configFetcher.fetchAsync()
                .thenApplyAsync(response -> {
                    if (response.isFetched()) {
                        this.configJsonCache.writeToCache(response.config());
                    }

                    if (!response.isFailed())
                        this.lastRefreshedTime = Instant.now();

                    if (this.initialized.compareAndSet(false, true)) {
                        this.init.complete(null);
                    }

                    this.isFetching.set(false);

                    return this.configJsonCache.readFromCache();
                });
    }

    private synchronized void broadcastConfigurationChanged() {
        for (ConfigurationChangeListener listener : this.listeners)
            listener.onConfigurationChanged();
    }

    @Override
    public void close() throws IOException {
        //APP
        if (pollingMode instanceof AutoPollingMode) {
            this.scheduler.shutdown();
            this.initScheduler.shutdown();
            this.listeners.clear();
        }
        // COMMON
        this.configFetcher.close();
    }

    public void setOnline() {
        this.offline = false;
        //TODO implement -
    }

    public void setOffline() {
        this.offline = true;
        //TODO implement - call fetcher
    }

    public boolean isOffline() {
        return offline;
    }
}
