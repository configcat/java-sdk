package com.configcat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class AutoPollingPolicy extends RefreshPolicyBase {
    private final ScheduledExecutorService scheduler;
    private final ScheduledExecutorService initScheduler;
    private final CompletableFuture<Void> initFuture;
    private final AtomicBoolean initialized;
    private final ArrayList<ConfigurationChangeListener> listeners;

    AutoPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, ConfigCatLogger logger, ConfigMemoryCache configMemoryCache, String sdkKey, AutoPollingMode config) {
        super(configFetcher, cache, logger, configMemoryCache, sdkKey);
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
                FetchResponse response = super.fetcher().getConfigurationAsync().get();
                Config cachedConfig = super.readConfigCache();
                Config fetchedConfig = response.config();
                if (response.isFetched() && !fetchedConfig.equals(cachedConfig)) {
                    super.writeConfigCache(fetchedConfig);
                    this.broadcastConfigurationChanged();
                }

                if (!this.initialized.getAndSet(true))
                    this.initFuture.complete(null);
            } catch (Exception e) {
                logger.error("Exception in AutoPollingCachePolicy", e);
            }
        }, 0, config.getAutoPollRateInSeconds(), TimeUnit.SECONDS);
    }

    @Override
    protected CompletableFuture<Config> getConfigurationAsync() {
        if (this.initFuture.isDone())
            return CompletableFuture.completedFuture(super.readConfigCache());

        return this.initFuture.thenApplyAsync(v -> super.readConfigCache());
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.scheduler.shutdown();
        this.initScheduler.shutdown();
        this.listeners.clear();
    }

    private synchronized void broadcastConfigurationChanged() {
        for (ConfigurationChangeListener listener : this.listeners)
            listener.onConfigurationChanged();
    }
}
