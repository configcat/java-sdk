package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class ConfigMemoryCache {
    private Config cached;
    private String cachedJson;
    private final Gson gson = new GsonBuilder().create();
    private final ConfigCatLogger logger;

    public ConfigMemoryCache(ConfigCatLogger logger) {
        this.logger = logger;
    }

    public Config getConfigFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        if (this.cachedJson != null && this.cachedJson == json) {
            return this.cached;
        }

        try {
            this.cached = this.gson.fromJson(json, Config.class);
            this.cached.jsonString = json;
            this.cachedJson = json;
            return cached;
        } catch (Exception e) {
            this.logger.error("Config JSON parsing failed.", e);
            return null;
        }

    }
}
