package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.MurmurHash3;

import java.util.Arrays;

class ConfigMemoryCache {
    private Config cached;
    private long[] cachedHash;
    private final Gson gson = new GsonBuilder().create();
    private final ConfigCatLogger logger;

    public ConfigMemoryCache(ConfigCatLogger logger) {
        this.logger = logger;
    }

    public Config getConfigFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        if (this.cachedHash != null) {
            long[] hash = MurmurHash3.hash128(json);
            if (Arrays.equals(this.cachedHash, hash)) {
                return this.cached;
            }
        }

        try {
            this.cached = this.gson.fromJson(json, Config.class);
            this.cached.jsonString = json;
            this.cachedHash = MurmurHash3.hash128(json);
            return cached;
        } catch (Exception e) {
            this.logger.error("Config JSON parsing failed.", e);
            return null;
        }

    }
}
