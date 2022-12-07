package com.configcat;

import java.util.HashMap;

class InMemoryCache extends ConfigCache {
    HashMap<String, String> map = new HashMap<>();

    @Override
    protected String read(String key) {
        return map.get(key);
    }

    @Override
    protected void write(String key, String value) {
        this.map.put(key, value);
    }

    public HashMap<String, String> getMap() {
        return map;
    }
}