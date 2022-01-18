package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

class ConfigJsonCache {
    private static final String CACHE_BASE = "java_" + ConfigFetcher.CONFIG_JSON_NAME + "_%s";
    private Config inMemoryConfig = Config.empty;
    private final Gson gson = new GsonBuilder().create();
    private final ConfigCatLogger logger;
    private final ConfigCache cache;
    private final String cacheKey;

    public ConfigJsonCache(ConfigCatLogger logger, ConfigCache cache, String sdkKey) {
        this.logger = logger;
        this.cache = cache;
        this.cacheKey = new String(Hex.encodeHex(DigestUtils.sha1(String.format(CACHE_BASE, sdkKey))));
    }

    public Config readFromJson(String json, String etag) {
        if (json == null || json.isEmpty()) {
            return Config.empty;
        }

        if (json.equals(this.inMemoryConfig.jsonString)) {
            return this.inMemoryConfig;
        }

        try {
            Config deserialized = this.deserialize(json);
            deserialized.eTag = etag;
            return deserialized;
        } catch (Exception e) {
            this.logger.error("Config JSON parsing failed.", e);
            return Config.empty;
        }
    }

    public Config readFromCache() {
        String fromCache = this.readCache();
        if (fromCache == null || fromCache.isEmpty() || fromCache.equals(this.inMemoryConfig.jsonString)) {
            return this.inMemoryConfig;
        }

        try {
            Config config = this.deserialize(fromCache);
            this.inMemoryConfig = config;
            return config;
        } catch (Exception e) {
            this.logger.error("Config JSON parsing failed.", e);
            return this.inMemoryConfig;
        }
    }

    public void writeToCache(Config config) {
        try {
            this.inMemoryConfig = config;
            this.cache.write(cacheKey, config.jsonString);
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache write.", e);
        }
    }

    private String readCache() {
        try {
            return this.cache.read(cacheKey);
        } catch (Exception e) {
            this.logger.error("An error occurred during the cache read.", e);
            return null;
        }
    }

    private Config deserialize(String json) {
        Config config = this.gson.fromJson(json, Config.class);
        config.jsonString = json;
        return config;
    }
}
