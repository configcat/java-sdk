package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

class ConfigJsonCache {
    private static final String CACHE_BASE = "java_" + ConfigFetcher.CONFIG_JSON_NAME + "_%s";
    private Config inMemoryConfig = Config.empty;
    private String inMemoryConfigString = "";
    private final Gson gson = new GsonBuilder().create();
    private final ConfigCatLogger logger;
    private final ConfigCache cache;
    private final String cacheKey;

    public ConfigJsonCache(ConfigCatLogger logger, ConfigCache cache, String sdkKey) {
        this.logger = logger;
        this.cache = cache;
        this.cacheKey = new String(Hex.encodeHex(DigestUtils.sha1(String.format(CACHE_BASE, sdkKey))));
    }

    public Config readFromJson(String json, String eTag) {
        if (json == null || json.isEmpty()) {
            return Config.empty;
        }

        try {
            Config config = this.deserialize(json);
            config.eTag = eTag;
            return config;
        } catch (Exception e) {
            this.logger.error("Config JSON parsing failed.", e);
            return Config.empty;
        }
    }

    public Config readFromCache() {
        String fromCache = this.readCache();
        if (fromCache == null || fromCache.isEmpty() || fromCache.equals(this.inMemoryConfigString)) {
            return this.inMemoryConfig;
        }

        try {
            Config config = this.deserialize(fromCache);
            this.inMemoryConfig = config;
            this.inMemoryConfigString = fromCache;
            return config;
        } catch (Exception e) {
            this.logger.error("Config JSON parsing failed.", e);
            return this.inMemoryConfig;
        }
    }

    public void writeToCache(Config config) {
        try {
            String configToCache = this.gson.toJson(config);
            this.inMemoryConfig = config;
            this.inMemoryConfigString = configToCache;
            this.cache.write(cacheKey, configToCache);
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
        return this.gson.fromJson(json, Config.class);
    }
}
