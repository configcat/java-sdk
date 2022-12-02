package com.configcat;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ConfigService implements Closeable {

    static final long DISTANT_FUTURE = Long.MAX_VALUE;
    static final long DISTANT_PAST = 0;

    private final ConfigFetcher configFetcher;
    private final ConfigJsonCache configJsonCache;
    private final ConfigCatLogger logger;
    private final PollingMode pollingMode;
    //APP
    private ScheduledExecutorService pollScheduler;
    private ScheduledExecutorService initScheduler;
    private ArrayList<ConfigurationChangeListener> listeners;
    private CompletableFuture<Result<Entry>> runningTask;
    //COMMON
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private boolean offline;
    private final ReentrantLock lock = new ReentrantLock(true);


    public ConfigService(String sdkKey, ConfigFetcher configFetcher, PollingMode pollingMode, ConfigCache configCache, ConfigCatLogger logger, boolean offline) {
        this.configFetcher = configFetcher;
        this.pollingMode = pollingMode;
        this.configJsonCache = new ConfigJsonCache(logger, configCache, sdkKey);
        this.logger = logger;
        this.offline = offline;
        if (pollingMode instanceof AutoPollingMode) {
            AutoPollingMode autoPollingMode = (AutoPollingMode) pollingMode;

            this.listeners = new ArrayList<>();
            if (autoPollingMode.getListener() != null) {
                this.listeners.add(autoPollingMode.getListener());
            }

            if (!offline) {
                startPoll(autoPollingMode);
            }

            this.initScheduler = Executors.newSingleThreadScheduledExecutor();
            this.initScheduler.schedule(() -> {
                if (initialized.compareAndSet(false, true)) {
                    String message = "Max init wait time for the very first fetch reached (" + autoPollingMode.getMaxInitWaitTimeSeconds() + "s). Returning cached config.";
                    logger.warn(message);
                    completeRunningTask(Result.error(message));
                }
            }, autoPollingMode.getMaxInitWaitTimeSeconds(), TimeUnit.SECONDS);

        } else {
            this.initialized.compareAndSet(false, true);
        }
    }

    private void startPoll(AutoPollingMode autoPollingMode) {
        long ageThreshold = (long) ((autoPollingMode.getAutoPollRateInSeconds() * 1000L) * 0.7);
        this.pollScheduler = Executors.newSingleThreadScheduledExecutor();
        this.pollScheduler.scheduleAtFixedRate(() -> fetchIfOlder(System.currentTimeMillis() - ageThreshold, false),
                0, autoPollingMode.getAutoPollRateInSeconds(), TimeUnit.SECONDS);
    }


    public CompletableFuture<Result<Entry>> refresh() {
        return fetchIfOlder(DISTANT_FUTURE, false);
    }

    public CompletableFuture<SettingResult> getSettings() {
        if (pollingMode instanceof LazyLoadingMode) {
            LazyLoadingMode lazyLoadingMode = (LazyLoadingMode) pollingMode;
            return fetchIfOlder(System.currentTimeMillis() - (lazyLoadingMode.getCacheRefreshIntervalInSeconds() * 1000L), false)
                    .thenApply(entryResult -> {
                        Entry result = entryResult.value();
                        return new SettingResult(result.config.entries, result.fetchTime);
                    });
        } else {
            return fetchIfOlder(DISTANT_PAST, true)
                    .thenApply(entryResult -> {
                        Entry result = entryResult.value();
                        return new SettingResult(result.config.entries, result.fetchTime);
                    });
        }

    }

    private CompletableFuture<Result<Entry>> fetchIfOlder(long time, boolean preferCached) {
        lock.lock();
        try {
            // Sync up with the cache and use it when it's not expired.
            Entry entryFromCache = configJsonCache.readFromCache();
            // Cache isn't expired
            if (entryFromCache.fetchTime > time ||
                    // Use cache anyway (get calls on auto & manual poll must not initiate fetch).
                    // The initialized check ensures that we subscribe for the ongoing fetch during the
                    // max init wait time window in case of auto poll.
                    preferCached && initialized.get() ||
                    // If we are in offline mode we are not allowed to initiate fetch. Return with cache
                    offline) {
                return CompletableFuture.completedFuture(Result.success(entryFromCache));
            }
            //Result.error("The SDK is in offline mode, it can't initiate HTTP calls."));

            if (runningTask == null) {
                // No fetch is running, initiate a new one.
                runningTask = new CompletableFuture<>();
                configFetcher.fetchAsync()
                        .thenAccept(this::processResponse);
            }

            return runningTask;

        } finally {
            lock.unlock();
        }
    }

    private synchronized void broadcastConfigurationChanged() {
        if (this.listeners != null) {
            for (ConfigurationChangeListener listener : this.listeners) {
                listener.onConfigurationChanged();
            }
        }
    }

    @Override
    public void close() throws IOException {
        //TODO atomic boolean here?
        if (pollingMode instanceof AutoPollingMode) {
            if (pollScheduler != null) this.pollScheduler.shutdown();
            if (initScheduler != null) this.initScheduler.shutdown();
            this.listeners.clear();
        }
        this.configFetcher.close();
    }

    public void setOnline() {
        lock.lock();
        try {
            if (!offline) return;
            offline = false;
            if (pollingMode instanceof AutoPollingMode) {
                startPoll((AutoPollingMode) pollingMode);
            }
            logger.debug("Switched to ONLINE mode.");
        } finally {
            lock.unlock();
        }
    }

    public void setOffline() {
        this.offline = true;
        //TODO implement - call fetcher - why?
        lock.lock();
        try {
            if (offline) return;
            offline = true;
            if (pollScheduler != null) pollScheduler.shutdown();
            if (initScheduler != null) initScheduler.shutdown();
            logger.debug("Switched to OFFLINE mode.");
        } finally {
            lock.unlock();
        }
    }

    public boolean isOffline() {
        return offline;
    }

    private void processResponse(FetchResponse response) {
        lock.lock();
        try {
            this.initialized.compareAndSet(false, true);
            if (response.isFetched()) {
                Entry entry = response.entry();
                configJsonCache.writeToCache(entry);
                this.broadcastConfigurationChanged();
                completeRunningTask(Result.success(entry));
            } else if (response.isNotModified()) {
                Entry entry = response.entry();
                entry.fetchTime = System.currentTimeMillis();
                configJsonCache.writeToCache(entry);
                completeRunningTask(Result.success(entry));
            } else {
                Entry entry = configJsonCache.readFromCache();
                //if actual fetch failed always use cache
                completeRunningTask(Result.success(entry));
            }
        } finally {
            lock.unlock();
        }
    }

    private void completeRunningTask(Result<Entry> result) {
        runningTask.complete(result);
        runningTask = null;
    }

}
