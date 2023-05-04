package com.configcat.cache;

public class SingleValueCache implements ConfigCache {
    private String value;

    public SingleValueCache(String value) {
        this.value = value;
    }

    @Override
    public String read(String key) {
        return this.value;
    }

    @Override
    public void write(String key, String value) {
        this.value = value;
    }
}