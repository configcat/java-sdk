package com.configcat;

class SingleValueCache extends ConfigCache {
    private String value;

    public SingleValueCache(String value) {
        this.value = value;
    }

    @Override
    protected String read(String key) {
        return this.value;
    }

    @Override
    protected void write(String key, String value) {
        this.value = value;
    }
}