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
public class AutoPollingPolicy extends RefreshPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoPollingPolicy.class);
    private static final ConfigurationParser parser = new ConfigurationParser();
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
    private AutoPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, Builder builder) {
        super(configFetcher, cache);
        super.fetcher().setMode("p");
        this.listeners = new ArrayList<>();

        if(builder.listener != null)
            this.listeners.add(builder.listener);

        this.initialized = new AtomicBoolean(false);
        this.initFuture = new CompletableFuture<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                FetchResponse response = super.fetcher().getConfigurationJsonStringAsync().get();
                String cached = super.cache().get();
                String config = response.config();
                if (response.isFetched() && !config.equals(cached)) {
                    super.cache().set(config);
                    this.broadcastConfigurationChanged(config);
                }

                if(!initialized.getAndSet(true))
                    initFuture.complete(null);

            } catch (Exception e){
                LOGGER.error("An error occurred during the scheduler poll execution", e);
            }
        }, 0, builder.autoPollIntervalInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        if(this.initFuture.isDone())
            return CompletableFuture.completedFuture(super.cache().get());

        return this.initFuture.thenApplyAsync(v -> super.cache().get());
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.scheduler.shutdown();
        this.listeners.clear();
    }

    /**
     * Subscribes a new listener to the configuration changed event.
     *
     * @param listener the listener.
     */
    public synchronized void addConfigurationChangeListener(ConfigurationChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a given listener from the configuration changed event.
     *
     * @param listener the listener.
     */
    public synchronized void removeConfigurationChangeListener(ConfigurationChangeListener listener) {
        listeners.remove(listener);
    }

    private synchronized void broadcastConfigurationChanged(String newConfiguration) {
        for (ConfigurationChangeListener listener : this.listeners)
            listener.onConfigurationChanged(parser, newConfiguration);
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
     * A builder that helps construct a {@link AutoPollingPolicy} instance.
     */
    public static class Builder {
        private int autoPollIntervalInSeconds = 60;
        private ConfigurationChangeListener listener;

        /**
         * Sets at least how often this policy should fetch the latest configuration and refresh the cache.
         *
         * @param autoPollIntervalInSeconds the poll interval in seconds.
         * @return the builder.
         * @throws IllegalArgumentException when the given value is less than 2 seconds.
         */
        public Builder autoPollIntervalInSeconds(int autoPollIntervalInSeconds) {
            if(autoPollIntervalInSeconds < 2)
                throw new IllegalArgumentException("autoPollRateInSeconds cannot be less than 2 seconds");

            this.autoPollIntervalInSeconds = autoPollIntervalInSeconds;
            return this;
        }

        /**
         * Sets a configuration changed listener.
         *
         * @param listener the listener.
         * @return the builder.
         * @throws IllegalArgumentException when the given listener is null.
         */
        public Builder configurationChangeListener(ConfigurationChangeListener listener) {
            if(listener == null)
                throw new IllegalArgumentException("listener cannot be null");

            this.listener = listener;
            return this;
        }

        /**
         * Builds the configured {@link AutoPollingPolicy} instance.
         *
         * @param configFetcher the internal config fetcher.
         * @param cache the internal cache.
         * @return the configured {@link AutoPollingPolicy} instance
         */
        public AutoPollingPolicy build(ConfigFetcher configFetcher, ConfigCache cache) {
            return new AutoPollingPolicy(configFetcher, cache, this);
        }
    }
}
