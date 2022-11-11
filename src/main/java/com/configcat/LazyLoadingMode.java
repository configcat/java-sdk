package com.configcat;

class LazyLoadingMode extends PollingMode {
    private final int cacheRefreshIntervalInSeconds;

    LazyLoadingMode(int cacheRefreshIntervalInSeconds) {
        this.cacheRefreshIntervalInSeconds = cacheRefreshIntervalInSeconds;
    }

    int getCacheRefreshIntervalInSeconds() {
        return cacheRefreshIntervalInSeconds;
    }


    @Override
    String getPollingIdentifier() {
        return "l";
    }
}
