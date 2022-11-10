package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

class ConfigJsonCache {
    private static final String CACHE_BASE = "java_" + ConfigFetcher.CONFIG_JSON_NAME + "_%s";
    private Entry inMemoryEntry = Entry.empty;
    private String inMemoryEntiryString = "";
    private final Gson gson = new GsonBuilder().create();
    private final ConfigCatLogger logger;
    private final ConfigCache cache;
    private final String cacheKey;

    public ConfigJsonCache(ConfigCatLogger logger, ConfigCache cache, String sdkKey) {
        this.logger = logger;
        this.cache = cache;
        this.cacheKey = new String(Hex.encodeHex(DigestUtils.sha1(String.format(CACHE_BASE, sdkKey))));
    }

    public Config readConfigFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Config.empty;
        }

        try {
            Config config = this.deserialize(json, Config.class);
            return config;
        } catch (Exception e) {
            this.logger.error("Config JSON parsing failed.", e);
            return Config.empty;
        }
    }

    public Entry readFromCache() {
        String fromCache = this.readCache();
        if (fromCache == null || fromCache.isEmpty() || fromCache.equals(this.inMemoryEntiryString)) {
            return this.inMemoryEntry;
        }

        try {
            Entry entry = this.deserialize(fromCache, Entry.class);
            this.inMemoryEntry = entry;
            this.inMemoryEntiryString = fromCache;
            return entry;
        } catch (Exception e) {
            this.logger.error("Entry JSON parsing failed.", e);
            return this.inMemoryEntry;
        }
    }

    public void writeToCache(Entry entry) {
        try {
            String configToCache = this.gson.toJson(entry);
            this.inMemoryEntry = entry;
            this.inMemoryEntiryString = configToCache;
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

    private <T> T deserialize(String json, Class<T> tClass) {
        return this.gson.fromJson(json, tClass);
    }
}
