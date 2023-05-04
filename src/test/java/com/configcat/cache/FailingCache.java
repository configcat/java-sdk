package com.configcat.cache;

public class FailingCache implements ConfigCache {

    @Override
    public String read(String key) throws CacheException {
        throw new CacheException();
    }

    @Override
    public void write(String key, String value) throws CacheException {
        throw new CacheException();
    }
}

