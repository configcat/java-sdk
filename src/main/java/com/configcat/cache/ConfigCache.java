package com.configcat.cache;

/**
 * A cache API used to make custom cache implementations for {@link com.configcat.ConfigCatClient}.
 */
public interface ConfigCache {
    /**
     * Child classes has to implement this method, the {@link com.configcat.ConfigCatClient}
     * uses it to get the actual value from the cache.
     *
     * @param key the key of the cache entry.
     * @return the cached configuration.
     * @throws CacheException if unable to read the cache.
     */
     String read(String key) throws CacheException;

    /**
     * * Child classes has to implement this method, the {@link com.configcat.ConfigCatClient}
     * uses it to set the actual cached value.
     *
     * @param key   the key of the cache entry.
     * @param value the new value to cache.
     * @throws CacheException if unable to save the value.
     */
     void write(String key, String value) throws CacheException;
}

