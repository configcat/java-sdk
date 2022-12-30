package com.configcat;

/**
 * Describes the polling modes.
 */
public final class PollingModes {

    private static final int DEFAULT_AUTO_POLL_INTERVAL_IN_SECONDS = 60;
    private static final int DEFAULT_MAX_INIT_WAIT_TIME_IN_SECONDS = 5;
    private static final int DEFAULT_CACHE_REFRESH_INTERVAL_IN_SECONDS = 60;


    /**
     * Set up the auto polling mode with default parameters.
     *
     * @return the auto polling mode.
     */
    public static PollingMode autoPoll() {
        return new AutoPollingMode(DEFAULT_AUTO_POLL_INTERVAL_IN_SECONDS, DEFAULT_MAX_INIT_WAIT_TIME_IN_SECONDS);
    }

    /**
     * Set up the auto polling mode with custom parameters.
     *
     * @param autoPollIntervalInSeconds Sets how often the config.json should be fetched and cached.
     * @return the auto polling mode.
     */
    public static PollingMode autoPoll(int autoPollIntervalInSeconds) {
        return new AutoPollingMode(autoPollIntervalInSeconds, DEFAULT_MAX_INIT_WAIT_TIME_IN_SECONDS);
    }

    /**
     * Set up the auto polling mode with custom parameters.
     *
     * @param autoPollIntervalInSeconds Sets how often the config.json should be fetched and cached.
     * @param maxInitWaitTimeSeconds    Sets the time limit between the initialization of the client and the first config.json acquisition.
     * @return the auto polling mode.
     */
    public static PollingMode autoPoll(int autoPollIntervalInSeconds, int maxInitWaitTimeSeconds) {
        return new AutoPollingMode(autoPollIntervalInSeconds, maxInitWaitTimeSeconds);
    }

    /**
     * Set up a lazy polling mode with default parameters.
     *
     * @return the lazy polling mode.
     */
    public static PollingMode lazyLoad() {
        return new LazyLoadingMode(DEFAULT_CACHE_REFRESH_INTERVAL_IN_SECONDS);
    }

    /**
     * Set up a lazy polling mode with custom parameters.
     *
     * @param cacheRefreshIntervalInSeconds Sets how long the cache will store its value before fetching the latest from the network again.
     * @return the lazy polling mode.
     */
    public static PollingMode lazyLoad(int cacheRefreshIntervalInSeconds) {
        return new LazyLoadingMode(cacheRefreshIntervalInSeconds);
    }

    /**
     * Set up the manual polling mode.
     *
     * @return the manual polling mode.
     */
    public static PollingMode manualPoll() {
        return new ManualPollingMode();
    }
}
