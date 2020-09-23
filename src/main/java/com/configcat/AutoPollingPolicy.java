package com.configcat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes a {@link RefreshPolicy} which polls the latest configuration
 * over HTTP and updates the local cache repeatedly.
 */
class AutoPollingPolicy extends RefreshPolicy {
    private final ScheduledExecutorService scheduler;
    private final CompletableFuture<Void> initFuture;
    private final AtomicBoolean initialized;
    private final ArrayList<ConfigurationChangeListener> listeners;

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache the internal cache instance.
     */
    AutoPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, AutoPollingMode modeConfig) {
        super(configFetcher, cache);
        this.listeners = new ArrayList<>();

        if(modeConfig.getListener() != null)
            this.listeners.add(modeConfig.getListener());

        this.initialized = new AtomicBoolean(false);
        this.initFuture = new CompletableFuture<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                FetchResponse response = super.fetcher().getConfigurationJsonStringAsync().get();
                String cached = super.readConfigCache();
                String config = response.config();
                if (response.isFetched() && !config.equals(cached)) {
                    super.writeConfigCache(config);
                    this.broadcastConfigurationChanged();
                }

                if(!initialized.getAndSet(true))
                    initFuture.complete(null);

            } catch (Exception e){
                LOGGER.error("Exception in AutoPollingCachePolicy", e);
            }
        }, 0, modeConfig.getAutoPollRateInSeconds(), TimeUnit.SECONDS);
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
