package com.configcat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * A cache API used to make custom cache implementations for {@link ConfigCatClient}.
 */
public abstract class ConfigCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigCache.class);
    private String inMemoryValue;

    public String get() {
        try {
            return this.read();
        } catch (Exception e) {
            LOGGER.error("An error occurred during the cache read", e);
            return this.inMemoryValue;
        }
    }

    public void set(String value) {
        try {
            this.inMemoryValue = value;
            this.write(value);
        } catch (Exception e) {
            LOGGER.error("An error occurred during the cache write", e);
        }
    }

    /**
     * Through this getter, the in-memory representation of the cached value can be accessed.
     * When the underlying cache implementations is not able to load or store its value,
     * this will represent the latest cached configuration.
     *
     * @return the cached value in memory.
     */
    public String inMemoryValue() { return this.inMemoryValue; }

    /**
     * Child classes has to implement this method, the {@link ConfigCatClient}
     * uses it to get the actual value from the cache.
     *
     * @return the cached configuration.
     * @throws Exception if unable to read the cache.
     */
    protected abstract String read() throws Exception;

    /**
     * * Child classes has to implement this method, the {@link ConfigCatClient}
     * uses it to set the actual cached value.
     *
     * @param value the new value to cache.
     * @throws Exception if unable to save the value.
     */
    protected abstract void write(String value) throws Exception;
}

