package com.configcat;

public class FailingCache extends ConfigCache {

    @Override
    protected String read(String key) throws Exception {
        throw new Exception();
    }

    @Override
    protected void write(String key, String value) throws Exception {
        throw new Exception();
    }
}
