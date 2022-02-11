package com.configcat;

/**
 * A null cache implementation.
 */
class NullConfigCache extends ConfigCache {

    @Override
    protected String read(String key) {
        return null;
    }

    @Override
    protected void write(String key, String value) { }
}
