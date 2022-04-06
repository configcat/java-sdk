package com.configcat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FailingWriteCache extends ConfigCache {
    Map<String, String> map = new HashMap<>();
    AtomicInteger counter = new AtomicInteger(1);
    public AtomicInteger successCounter = new AtomicInteger(0);

    @Override
    protected String read(String key) {
        return map.get(key);
    }

    @Override
    protected void write(String key, String value) throws Exception {
        if(counter.getAndIncrement() % 2 == 0){
            throw new Exception();
        }

        successCounter.incrementAndGet();
        map.put(key, value);
    }
}
