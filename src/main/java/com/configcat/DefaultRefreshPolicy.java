package com.configcat;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

abstract class DefaultRefreshPolicy implements RefreshPolicy {
    private static final String CacheBase = "java_" + ConfigFetcher.CONFIG_JSON_NAME + "_%s";
    private final ConfigCache cache;
    private final ConfigFetcher configFetcher;
    protected final ConfigMemoryCache configMemoryCache;
    private final String CacheKey;
    protected final ConfigCatLogger logger;
    private Config inMemoryConfig;

    protected Config readConfigCache() {
        try {
            Config result = this.configMemoryCache.getConfigFromJson(this.cache.read(CacheKey));
            return result != null ? result : this.inMemoryConfig;
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache read.", e);
            return this.inMemoryConfig;
        }
    }

    protected void writeConfigCache(Config value) {
        try {
            this.inMemoryConfig = value;
            this.cache.write(CacheKey, value.JsonString);
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache write.", e);
        }
    }

    protected ConfigFetcher fetcher() {
        return configFetcher;
    }

    DefaultRefreshPolicy(ConfigFetcher configFetcher, ConfigCache cache, ConfigCatLogger logger, ConfigMemoryCache configMemoryCache, String sdkKey) {
        this.configFetcher = configFetcher;
        this.cache = cache;
        this.logger = logger;
        this.configMemoryCache = configMemoryCache;
        this.CacheKey = new String(Hex.encodeHex(DigestUtils.sha1(String.format(CacheBase, sdkKey))));
    }

    public abstract CompletableFuture<Config> getConfigurationAsync();

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
