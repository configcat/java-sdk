package com.configcat;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ConfigService implements Closeable {

    private static final String CACHE_BASE = "java_" + Constants.CONFIG_JSON_NAME + "_%s";

    private Entry cachedEntry = Entry.EMPTY;
    private String cachedEntryString = "";
    private final ConfigCache cache;

    private final String cacheKey;
    private final ConfigFetcher configFetcher;
    private final ConfigCatLogger logger;
    private final PollingMode pollingMode;
    private ScheduledExecutorService pollScheduler;
    private ScheduledExecutorService initScheduler;
    private CompletableFuture<Result<Entry>> runningTask;
    private boolean initialized = false;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean offline;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final ConfigCatHooks configCatHooks;


    public ConfigService(String sdkKey,
                         ConfigFetcher configFetcher,
                         PollingMode pollingMode,
                         ConfigCache cache,
                         ConfigCatLogger logger,
                         boolean offline,
                         ConfigCatHooks configCatHooks) {
        this.configFetcher = configFetcher;
        this.pollingMode = pollingMode;
        this.cacheKey = new String(Hex.encodeHex(DigestUtils.sha1(String.format(CACHE_BASE, sdkKey))));
        this.cache = cache;
        this.logger = logger;
        this.offline = new AtomicBoolean(offline);
        this.configCatHooks = configCatHooks;

        if (pollingMode instanceof AutoPollingMode && !offline) {
            AutoPollingMode autoPollingMode = (AutoPollingMode) pollingMode;

            startPoll(autoPollingMode);

            this.initScheduler = Executors.newSingleThreadScheduledExecutor();
            this.initScheduler.schedule(() -> {
                lock.lock();
                try {
                    if (!initialized) {
                        initialized = true;
                        this.configCatHooks.invokeOnClientReady();
                        String message = "Max init wait time for the very first fetch reached (" + autoPollingMode.getMaxInitWaitTimeSeconds() + "s). Returning cached config.";
                        this.logger.warn(message);
                        completeRunningTask(Result.error(message, cachedEntry));
                    }
                } finally {
                    lock.unlock();
                }
            }, autoPollingMode.getMaxInitWaitTimeSeconds(), TimeUnit.SECONDS);

        } else {
            setInitialized();
        }
    }

    private void setInitialized() {
        if (!initialized) {
            initialized = true;
            configCatHooks.invokeOnClientReady();
        }
    }

    private void startPoll(AutoPollingMode autoPollingMode) {
        long ageThreshold = (long) ((autoPollingMode.getAutoPollRateInSeconds() * 1000L) * 0.7);
        this.pollScheduler = Executors.newSingleThreadScheduledExecutor();
        this.pollScheduler.scheduleAtFixedRate(() -> fetchIfOlder(System.currentTimeMillis() - ageThreshold, false),
                0, autoPollingMode.getAutoPollRateInSeconds(), TimeUnit.SECONDS);
    }


    public CompletableFuture<RefreshResult> refresh() {
        if (offline.get()) {
            String offlineWarning = "Client is in offline mode, it can't initiate HTTP calls.";
            logger.warn(offlineWarning);
            return CompletableFuture.completedFuture(new RefreshResult(false, offlineWarning));
        }
        return fetchIfOlder(Constants.DISTANT_FUTURE, false)
                .thenApply(entryResult -> new RefreshResult(entryResult.error() == null, entryResult.error()));
    }

    public CompletableFuture<SettingResult> getSettings() {
        if (pollingMode instanceof LazyLoadingMode) {
            LazyLoadingMode lazyLoadingMode = (LazyLoadingMode) pollingMode;
            return fetchIfOlder(System.currentTimeMillis() - (lazyLoadingMode.getCacheRefreshIntervalInSeconds() * 1000L), false)
                    .thenApply(entryResult -> new SettingResult(entryResult.value().getConfig().getEntries(), entryResult.value().getFetchTime()));
        } else {
            return fetchIfOlder(Constants.DISTANT_PAST, true)
                    .thenApply(entryResult -> new SettingResult(entryResult.value().getConfig().getEntries(), entryResult.value().getFetchTime()));
        }

    }

    private CompletableFuture<Result<Entry>> fetchIfOlder(long time, boolean preferCached) {
        lock.lock();
        try {
            // Sync up with the cache and use it when it's not expired.
            if (cachedEntry.isEmpty() || cachedEntry.getFetchTime() > time) {
                Entry fromCache = readCache();
                if (!fromCache.isEmpty() && !fromCache.getETag().equals(cachedEntry.getETag())) {
                    configCatHooks.invokeOnConfigChanged(fromCache.getConfig().getEntries());
                    cachedEntry = fromCache;
                }
                // Cache isn't expired
                if (cachedEntry.getFetchTime() > time) {
                    setInitialized();
                    return CompletableFuture.completedFuture(Result.success(cachedEntry));
                }
            }
            // Use cache anyway (get calls on auto & manual poll must not initiate fetch).
            // The initialized check ensures that we subscribe for the ongoing fetch during the
            // max init wait time window in case of auto poll.
            if (preferCached && initialized) {
                return CompletableFuture.completedFuture(Result.success(cachedEntry));
            }
            // If we are in offline mode we are not allowed to initiate fetch.
            if (offline.get()) {
                return CompletableFuture.completedFuture(Result.success(cachedEntry));
            }

            if (runningTask == null) {
                // No fetch is running, initiate a new one.
                runningTask = new CompletableFuture<>();
                configFetcher.fetchAsync(cachedEntry.getETag())
                        .thenAccept(this::processResponse);
            }

            return runningTask;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        if (pollingMode instanceof AutoPollingMode) {
            if (pollScheduler != null) this.pollScheduler.shutdown();
            if (initScheduler != null) this.initScheduler.shutdown();
        }
        this.configFetcher.close();
    }

    public void setOnline() {
        if (closed.get()) {
            logger.warn("Client has already been closed, this 'setOnline' has no effect.");
        }
        lock.lock();
        try {
            if (!offline.compareAndSet(true, false)) return;
            if (pollingMode instanceof AutoPollingMode) {
                startPoll((AutoPollingMode) pollingMode);
            }
            logger.debug("Switched to ONLINE mode.");
        } finally {
            lock.unlock();
        }
    }

    public void setOffline() {
        if (closed.get()) {
            logger.warn("Client has already been closed, this 'setOffline' has no effect.");
        }
        lock.lock();
        try {
            if (!offline.compareAndSet(false, true)) return;
            if (pollScheduler != null) pollScheduler.shutdown();
            if (initScheduler != null) initScheduler.shutdown();
            logger.debug("Switched to OFFLINE mode.");
        } finally {
            lock.unlock();
        }
    }

    public boolean isOffline() {
        return offline.get();
    }

    private void processResponse(FetchResponse response) {
        lock.lock();
        try {
            setInitialized();
            if (response.isFetched()) {
                Entry entry = response.entry();
                cachedEntry = entry;
                writeCache(entry);
                completeRunningTask(Result.success(entry));
                configCatHooks.invokeOnConfigChanged(entry.getConfig().getEntries());
            } else if (response.isNotModified()) {
                if (response.isFetchTimeUpdatable()) {
                    cachedEntry = cachedEntry.withFetchTime(System.currentTimeMillis());
                }
                writeCache(cachedEntry);
                completeRunningTask(Result.success(cachedEntry));
            } else {
                if (response.isFetchTimeUpdatable()) {
                    cachedEntry = cachedEntry.withFetchTime(System.currentTimeMillis());
                }
                //if actual fetch failed always use cache
                completeRunningTask(Result.error(response.error(), cachedEntry));
            }
        } finally {
            lock.unlock();
        }
    }

    private void completeRunningTask(Result<Entry> result) {
        runningTask.complete(result);
        runningTask = null;
    }

    private Entry readCache() {
        try {
            String json = cache.read(cacheKey);
            if (json != null && json.equals(cachedEntryString)) {
                return Entry.EMPTY;
            }
            cachedEntryString = json;
            Entry deserialized = Utils.gson.fromJson(json, Entry.class);
            return deserialized == null || deserialized.getConfig() == null ? Entry.EMPTY : deserialized;
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache read.", e);
            return Entry.EMPTY;
        }
    }

    private void writeCache(Entry entry) {
        try {
            String configToCache = Utils.gson.toJson(entry);
            cachedEntryString = configToCache;
            cache.write(cacheKey, configToCache);
        } catch (Exception e) {
            logger.error("An error occurred during the cache write.", e);
        }
    }
}
