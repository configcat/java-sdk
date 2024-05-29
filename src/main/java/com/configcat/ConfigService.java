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

    private static final String CACHE_BASE = "%s_" + Constants.CONFIG_JSON_NAME + "_" + Constants.SERIALIZATION_FORMAT_VERSION;

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
    private final AtomicBoolean initialized = new AtomicBoolean(false);
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
                    if (initialized.compareAndSet(false, true)) {
                        this.configCatHooks.invokeOnClientReady(determineReadyState());
                        String message = ConfigCatLogMessages.getAutoPollMaxInitWaitTimeReached(autoPollingMode.getMaxInitWaitTimeSeconds());
                        this.logger.warn(4200, message);
                        completeRunningTask(Result.error(message, cachedEntry));
                    }
                } finally {
                    lock.unlock();
                }
            }, autoPollingMode.getMaxInitWaitTimeSeconds(), TimeUnit.SECONDS);

        } else {
            // Sync up with cache before reporting ready state
            cachedEntry = readCache();
            setInitialized();
        }
    }

    private void setInitialized() {
        if (initialized.compareAndSet(false, true)) {
            configCatHooks.invokeOnClientReady(determineReadyState());
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
            String offlineWarning = ConfigCatLogMessages.CONFIG_SERVICE_CANNOT_INITIATE_HTTP_CALLS_WARN;
            logger.warn(3200, offlineWarning);
            return CompletableFuture.completedFuture(new RefreshResult(false, offlineWarning));
        }
        return fetchIfOlder(Constants.DISTANT_FUTURE, false)
                .thenApply(entryResult -> new RefreshResult(entryResult.error() == null, entryResult.error()));
    }

    public CompletableFuture<SettingResult> getSettings() {
        if (pollingMode instanceof LazyLoadingMode) {
            LazyLoadingMode lazyLoadingMode = (LazyLoadingMode) pollingMode;
            return fetchIfOlder(System.currentTimeMillis() - (lazyLoadingMode.getCacheRefreshIntervalInSeconds() * 1000L), false)
                    .thenApply(entryResult -> !entryResult.value().isEmpty()
                            ? new SettingResult(entryResult.value().getConfig().getEntries(), entryResult.value().getFetchTime())
                            : SettingResult.EMPTY);
        } else {
            long threshold = Constants.DISTANT_PAST;
            if (!initialized.get() && pollingMode instanceof AutoPollingMode) {
                AutoPollingMode autoPollingMode = (AutoPollingMode) pollingMode;
                threshold = System.currentTimeMillis() - (autoPollingMode.getAutoPollRateInSeconds() * 1000L);
            }
            return fetchIfOlder(threshold, initialized.get()) // If we are initialized, we prefer the cached results
                    .thenApply(entryResult -> !entryResult.value().isEmpty()
                            ? new SettingResult(entryResult.value().getConfig().getEntries(), entryResult.value().getFetchTime())
                            : SettingResult.EMPTY);
        }

    }

    private CompletableFuture<Result<Entry>> fetchIfOlder(long threshold, boolean preferCached) {
        lock.lock();
        try {
            // Sync up with the cache and use it when it's not expired.
            Entry fromCache = readCache();
            if (!fromCache.isEmpty() && !fromCache.getETag().equals(cachedEntry.getETag())) {
                configCatHooks.invokeOnConfigChanged(fromCache.getConfig().getEntries());
                cachedEntry = fromCache;
            }
            // Cache isn't expired
            if (!cachedEntry.isExpired(threshold)) {
                setInitialized();
                return CompletableFuture.completedFuture(Result.success(cachedEntry));
            }
            // If we are in offline mode or the caller prefers cached values, do not initiate fetch.
            if (offline.get() || preferCached) {
                return CompletableFuture.completedFuture(Result.success(cachedEntry));
            }

            if (runningTask == null) { // No fetch is running, initiate a new one.
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
        lock.lock();
        try {
            if (!offline.compareAndSet(true, false)) return;
            if (pollingMode instanceof AutoPollingMode) {
                startPoll((AutoPollingMode) pollingMode);
            }
            logger.info(5200, ConfigCatLogMessages.getConfigServiceStatusChanged("ONLINE"));
        } finally {
            lock.unlock();
        }
    }

    public void setOffline() {
        lock.lock();
        try {
            if (!offline.compareAndSet(false, true)) return;
            if (pollScheduler != null) pollScheduler.shutdown();
            if (initScheduler != null) initScheduler.shutdown();
            logger.info(5200, ConfigCatLogMessages.getConfigServiceStatusChanged("OFFLINE"));
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
            if (response.isFetched()) {
                Entry entry = response.entry();
                cachedEntry = entry;
                writeCache(entry);
                configCatHooks.invokeOnConfigChanged(entry.getConfig().getEntries());
                completeRunningTask(Result.success(entry));
            } else {
                if (response.isFetchTimeUpdatable()) {
                    cachedEntry = cachedEntry.withFetchTime(System.currentTimeMillis());
                    writeCache(cachedEntry);
                }
                completeRunningTask(response.isFailed()
                        ? Result.error(response.error(), cachedEntry)
                        : Result.success(cachedEntry));
            }
            setInitialized();
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
            String cachedConfigJson = cache.read(cacheKey);
            if (cachedConfigJson != null && cachedConfigJson.equals(cachedEntryString)) {
                return Entry.EMPTY;
            }
            cachedEntryString = cachedConfigJson;
            Entry deserialized = Entry.fromString(cachedConfigJson);
            return deserialized == null || deserialized.getConfig() == null ? Entry.EMPTY : deserialized;
        } catch (Exception e) {
            this.logger.error(2200, ConfigCatLogMessages.CONFIG_SERVICE_CACHE_READ_ERROR, e);
            return Entry.EMPTY;
        }
    }

    private void writeCache(Entry entry) {
        try {
            String configToCache = entry.serialize();
            cachedEntryString = configToCache;
            cache.write(cacheKey, configToCache);
        } catch (Exception e) {
            logger.error(2201, ConfigCatLogMessages.CONFIG_SERVICE_CACHE_WRITE_ERROR, e);
        }
    }

    private ClientReadyState determineReadyState(){
        if(cachedEntry.isEmpty()) {
            return ClientReadyState.NO_FLAG_DATA;
        }
        if(pollingMode instanceof ManualPollingMode) {
            return ClientReadyState.HAS_CACHED_FLAG_DATA_ONLY;
        } else if(pollingMode instanceof LazyLoadingMode) {
            if(cachedEntry.isExpired(System.currentTimeMillis() - (((LazyLoadingMode)pollingMode).getCacheRefreshIntervalInSeconds() * 1000L))) {
                return ClientReadyState.HAS_CACHED_FLAG_DATA_ONLY;
            }
        } else if(pollingMode instanceof AutoPollingMode) {
            if(cachedEntry.isExpired(System.currentTimeMillis() - (((AutoPollingMode)pollingMode).getAutoPollRateInSeconds() * 1000L))) {
                return ClientReadyState.HAS_CACHED_FLAG_DATA_ONLY;
            }
        }
        return ClientReadyState.HAS_UP_TO_DATE_FLAG_DATA;
    }
}
