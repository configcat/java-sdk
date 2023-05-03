package com.configcat.cache;

/**
 * A null cache implementation.
 */
public class NullConfigCache extends ConfigCache {

    @Override
    public String read(String key) {
        return null;
    }

    @Override
    public void write(String key, String value) {
    }
}
