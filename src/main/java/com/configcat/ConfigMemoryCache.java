package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class ConfigMemoryCache {
    private Config cached;
    private final Gson gson = new GsonBuilder().create();
    private final ConfigCatLogger logger;

    public ConfigMemoryCache(ConfigCatLogger logger) {
        this.logger = logger;
    }

    public Config getConfigFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        if (this.cached != null && json.equals(this.cached.jsonString)) {
            return this.cached;
        }

        try {
            Config config = this.gson.fromJson(json, Config.class);
            config.jsonString = json;
            this.cached = config;
            return config;
        } catch (Exception e) {
            this.logger.error("Config JSON parsing failed.", e);
            return null;
        }

    }
}
