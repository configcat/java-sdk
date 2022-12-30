package com.configcat;

import java.util.function.Supplier;

final class Helpers {

    static String entryStringFromConfigString(String json) {
        Config config = Utils.gson.fromJson(json, Config.class);
        return entryToJson(new Entry(config, "fakeTag", System.currentTimeMillis()));
    }

    static String entryToJson(Entry entry) {
        return Utils.gson.toJson(entry);
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
