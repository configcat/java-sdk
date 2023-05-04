package com.configcat.cache;

import java.util.HashMap;

public class InMemoryCache implements ConfigCache {
    HashMap<String, String> map = new HashMap<>();

    @Override
    public String read(String key) {
        return map.get(key);
    }

    @Override
    public void write(String key, String value) {
        this.map.put(key, value);
    }

    public HashMap<String, String> getMap() {
        return map;
    }
}