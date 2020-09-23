package com.configcat;

import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory cache implementation used to store the fetched configurations.
 */
class InMemoryConfigCache extends ConfigCache {
    private final Map<String, String> map = new HashMap<>();

    @Override
    protected String read(String key) {
        return this.map.get(key);
    }

    @Override
    protected void write(String key, String value) {
        this.map.put(key, value);
    }
}
