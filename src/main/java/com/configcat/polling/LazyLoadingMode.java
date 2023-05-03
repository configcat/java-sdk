package com.configcat.polling;

public class LazyLoadingMode extends PollingMode {
    private final int cacheRefreshIntervalInSeconds;

    LazyLoadingMode(int cacheRefreshIntervalInSeconds) {
        this.cacheRefreshIntervalInSeconds = cacheRefreshIntervalInSeconds;
    }

    public int getCacheRefreshIntervalInSeconds() {
        return cacheRefreshIntervalInSeconds;
    }


    @Override
    public String getPollingIdentifier() {
        return "l";
    }
}
