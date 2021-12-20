package com.configcat;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

interface RefreshPolicy extends Closeable {
    CompletableFuture<Map<String, Setting>> getSettingsAsync();
    CompletableFuture<Void> refreshAsync();
}

abstract class RefreshPolicyBase implements RefreshPolicy {
    private static final String CACHE_BASE = "java_" + ConfigFetcher.CONFIG_JSON_NAME + "_%s";
    private final ConfigCache cache;
    private final ConfigFetcher configFetcher;
    protected final ConfigMemoryCache configMemoryCache;
    private final String cacheKey;
    protected final ConfigCatLogger logger;
    private Config inMemoryConfig;

    protected Config readConfigCache() {
        try {
            Config result = this.configMemoryCache.getConfigFromJson(this.cache.read(cacheKey));
            return result != null ? result : this.inMemoryConfig;
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache read.", e);
            return this.inMemoryConfig;
        }
    }

    protected void writeConfigCache(Config value) {
        try {
            this.inMemoryConfig = value;
            this.cache.write(cacheKey, value.jsonString);
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache write.", e);
        }
    }

    protected ConfigFetcher fetcher() {
        return configFetcher;
    }

    RefreshPolicyBase(ConfigFetcher configFetcher, ConfigCache cache, ConfigCatLogger logger, ConfigMemoryCache configMemoryCache, String sdkKey) {
        this.configFetcher = configFetcher;
        this.cache = cache;
        this.logger = logger;
        this.configMemoryCache = configMemoryCache;
        this.cacheKey = new String(Hex.encodeHex(DigestUtils.sha1(String.format(CACHE_BASE, sdkKey))));
    }

    public CompletableFuture<Map<String, Setting>> getSettingsAsync() {
        return this.getConfigurationAsync()
                .thenApply(config -> {
                   if (config == null) {
                       return new HashMap<>();
                   }
                   return config.entries;
                });
    }

    protected abstract CompletableFuture<Config> getConfigurationAsync();

    public CompletableFuture<Void> refreshAsync() {
        return this.fetcher().getConfigurationAsync()
                .thenAcceptAsync(response -> {
                    if (response.isFetched()) {
                        this.writeConfigCache(response.config());
                    }
                });
    }

    @Override
    public void close() throws IOException {
        this.configFetcher.close();
    }
}

class NullRefreshPolicy implements RefreshPolicy {
    @Override
    public CompletableFuture<Map<String, Setting>> getSettingsAsync() {
        return CompletableFuture.completedFuture(new HashMap<>());
    }

    @Override
    public CompletableFuture<Void> refreshAsync() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() throws IOException { }
}
