package com.configcat;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class AutoPollingPolicy extends RefreshPolicy {
    private final ScheduledExecutorService scheduler;
    private final CompletableFuture<Void> initFuture;
    private final AtomicBoolean initialized;
    private final ArrayList<ConfigurationChangeListener> listeners;

    AutoPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, Logger logger, String sdkKey, AutoPollingMode config) {
        super(configFetcher, cache, logger, sdkKey);
        this.listeners = new ArrayList<>();

        if(config.getListener() != null)
            this.listeners.add(config.getListener());

        this.initialized = new AtomicBoolean(false);
        this.initFuture = new CompletableFuture<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                FetchResponse response = super.fetcher().getConfigurationJsonStringAsync().get();
                String cached = super.readConfigCache();
                String configJson = response.config();
                if (response.isFetched() && !configJson.equals(cached)) {
                    super.writeConfigCache(configJson);
                    this.broadcastConfigurationChanged();
                }

                if(!initialized.getAndSet(true))
                    initFuture.complete(null);

            } catch (Exception e){
                logger.error("Exception in AutoPollingCachePolicy", e);
            }
        }, 0, config.getAutoPollRateInSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        if(this.initFuture.isDone())
            return CompletableFuture.completedFuture(super.readConfigCache());

        return this.initFuture.thenApplyAsync(v -> super.readConfigCache());
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.scheduler.shutdown();
        this.listeners.clear();
    }

    private synchronized void broadcastConfigurationChanged() {
        for (ConfigurationChangeListener listener : this.listeners)
            listener.onConfigurationChanged();
    }
}
