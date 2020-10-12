package com.configcat;

/**
 * A cache API used to make custom cache implementations for {@link ConfigCatClient}.
 */
public abstract class ConfigCache {
    /**
     * Child classes has to implement this method, the {@link ConfigCatClient}
     * uses it to get the actual value from the cache.
     *
     * @param key the key of the cache entry.
     * @return the cached configuration.
     * @throws Exception if unable to read the cache.
     */
    protected abstract String read(String key) throws Exception;

    /**
     * * Child classes has to implement this method, the {@link ConfigCatClient}
     * uses it to set the actual cached value.
     *
     * @param key the key of the cache entry.
     * @param value the new value to cache.
     * @throws Exception if unable to save the value.
     */
    protected abstract void write(String key, String value) throws Exception;
}

