package com.configcat;

import java.io.Closeable;
import java.io.IOException;
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
    private int cacheRefreshIntervalInSeconds;
    private AtomicBoolean isFetching;
    private CompletableFuture<Entry> fetchingFuture;
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
            //TODO add offline here
            AutoPollingMode autoPollingMode = (AutoPollingMode) pollingMode;

            //TODO add listener, this still should work. hooks will replace it
            this.listeners = new ArrayList<>();
            if (autoPollingMode.getListener() != null)
                this.listeners.add(autoPollingMode.getListener());


            this.initialized = new AtomicBoolean(false);
            //TODO no initFuture - just one runningTask
            this.initFuture = new CompletableFuture<>();
            //TODO startPoll
            startPolling(autoPollingMode);

            this.initScheduler = Executors.newSingleThreadScheduledExecutor();
            this.initScheduler.schedule(() -> {
                if (!this.initialized.getAndSet(true))
                    this.initFuture.complete(null);
            }, autoPollingMode.getMaxInitWaitTimeSeconds(), TimeUnit.SECONDS);

        } else if (pollingMode instanceof LazyLoadingMode) {
            //TODO simple else not esle if
            LazyLoadingMode config = (LazyLoadingMode) pollingMode;
            //TODO move this check to getSettings
            this.cacheRefreshIntervalInSeconds = config.getCacheRefreshIntervalInSeconds();
            this.isFetching = new AtomicBoolean(false);
            this.initialized = new AtomicBoolean(false);
            this.initFuture = new CompletableFuture<>();
        }
        // nothing to do if MPM -- handle in else
    }

    private void startPolling(AutoPollingMode autoPollingMode) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                //TODO fetch if older
                FetchResponse response = this.configFetcher.fetchAsync().get();
                if (response.isFetched()) {
                    this.configJsonCache.writeToCache(response.entry());
                    this.broadcastConfigurationChanged();
                }
                //TODO remove init from here
                if (!this.initialized.getAndSet(true))
                    this.initFuture.complete(null);
            } catch (Exception e) {
                logger.error("Exception in AutoPollingCachePolicy", e);
            }
        }, 0, autoPollingMode.getAutoPollRateInSeconds(), TimeUnit.SECONDS);
    }

    public CompletableFuture<Void> refreshAsync() {
        return this.configFetcher.fetchAsync()
                .thenAcceptAsync(response -> {
                    if (response.isFetched()) {
                        this.configJsonCache.writeToCache(response.entry());
                    }
                });
    }

    public CompletableFuture<Map<String, Setting>> getSettingsAsync() {
        //TODO implement
        if (pollingMode instanceof AutoPollingMode) {
            if (this.initFuture.isDone())
                return CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(entry -> entry.config.entries);

            return this.initFuture.thenApplyAsync(v -> this.configJsonCache.readFromCache()).thenApply(entry -> entry.config.entries);
        }
        if (pollingMode instanceof LazyLoadingMode) {
            //TODO replace with entry fetch time
            //if (Instant.now().isAfter(lastRefreshedTime.plusSeconds(this.cacheRefreshIntervalInSeconds))) {
            boolean isInitialized = this.initFuture.isDone();

            if (isInitialized && !this.isFetching.compareAndSet(false, true))
                return this.initialized.get()
                        ? CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(entry -> entry.config.entries)
                        : this.fetchingFuture.thenApply(entry -> entry.config.entries);

            logger.debug("Cache expired, refreshing.");
            if (isInitialized) {
                this.fetchingFuture = this.fetch();
                return this.fetchingFuture.thenApply(entry -> entry.config.entries);
            } else {
                if (this.isFetching.compareAndSet(false, true)) {
                    this.fetchingFuture = this.fetch();
                }
                return this.initFuture.thenApplyAsync(v -> this.configJsonCache.readFromCache()).thenApply(entry -> entry.config.entries);
            }
            //}
            //return CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(entry -> entry.config.entries);
        }
        //MPP the last
        return CompletableFuture.completedFuture(this.configJsonCache.readFromCache()).thenApply(entry -> entry.config.entries);
    }

    // LPP fetch
    private CompletableFuture<Entry> fetch() {
        //TODO fetch should check fetchtime from stored entry
        //TODO add runnuing task. part of Init?
        return this.configFetcher.fetchAsync()
                .thenApplyAsync(response -> {
                    if (response.isFetched()) {
                        this.configJsonCache.writeToCache(response.entry());
                    }

                    //TODO entry fetch time handles
                    //if (!response.isFailed())
                    //   this.lastRefreshedTime = Instant.now();

                    if (this.initialized.compareAndSet(false, true)) {
                        this.initFuture.complete(null);
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
        if (pollingMode instanceof AutoPollingMode) {
            this.scheduler.shutdown();
            this.initScheduler.shutdown();
            this.listeners.clear();
        }
        this.configFetcher.close();
    }

    public void setOnline() {
        this.offline = false;
        //TODO implement -
        // TODO call startPolling
    }

    public void setOffline() {
        this.offline = true;
        //TODO implement - call fetcher
        //TODO stop scheduler
    }

    public boolean isOffline() {
        return offline;
    }
}
