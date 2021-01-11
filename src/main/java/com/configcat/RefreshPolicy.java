package com.configcat;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

abstract class RefreshPolicy implements Closeable {
    private static final String CacheBase = "java_"+ ConfigFetcher.CONFIG_JSON_NAME +"_%s";
    private final ConfigCache cache;
    private final ConfigFetcher configFetcher;
    private final String CacheKey;
    protected final Logger logger;
    private String inMemoryConfig;

    protected String readConfigCache() {
        try {
            return this.cache.read(CacheKey);
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache read.", e);
            return this.inMemoryConfig;
        }
    }

    protected void writeConfigCache(String value) {
        try {
            this.inMemoryConfig = value;
            this.cache.write(CacheKey, value);
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache write.", e);
        }
    }

    protected ConfigFetcher fetcher() {
        return configFetcher;
    }

    RefreshPolicy(ConfigFetcher configFetcher, ConfigCache cache, Logger logger, String sdkKey) {
        this.configFetcher = configFetcher;
        this.cache = cache;
        this.logger = logger;
        this.CacheKey = new String(Hex.encodeHex(DigestUtils.sha1(String.format(CacheBase, sdkKey))));
    }

    public abstract CompletableFuture<String> getConfigurationJsonAsync();

    public CompletableFuture<Void> refreshAsync() {
        return this.fetcher().getConfigurationJsonStringAsync()
                .thenAcceptAsync(response -> {
                    if(response.isFetched())
                        this.writeConfigCache(response.config());
                });
    }

    String getLatestCachedValue() {
        return this.inMemoryConfig;
    }

    @Override
    public void close() throws IOException {
        this.configFetcher.close();
    }
}
