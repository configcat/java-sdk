package com.configcat;

import java.util.function.Supplier;

final class Helpers {

    static String cacheValueFromConfigJson(String json) {
        Config config = Utils.gson.fromJson(json, Config.class);
        Entry entry = new Entry(config, "fakeTag", json, CacheUtils.DateTimeUtils.format(System.currentTimeMillis()));
        return CacheUtils.serialize(entry);
    }

    static void waitFor(Supplier<Boolean> predicate) throws InterruptedException {
        waitFor(3000, predicate);
    }

    static void waitFor(long timeout, Supplier<Boolean> predicate) throws InterruptedException {
        long end = System.currentTimeMillis() + timeout;
        while (!predicate.get()) {
            Thread.sleep(200);
            if (System.currentTimeMillis() > end) {
                throw new RuntimeException("Test timed out.");
            }
        }
    }
}
