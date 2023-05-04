package com.configcat.cache;

public class FailingCache implements ConfigCache {

    @Override
    public String read(String key) throws Exception {
        throw new Exception();
    }

    @Override
    public void write(String key, String value) throws Exception {
        throw new Exception();
    }
}

