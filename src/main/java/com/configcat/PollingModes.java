package com.configcat;

/**
 * Describes the polling modes.
 */
public final class PollingModes {

    private static final int DEFAULT_AUTO_POLL_INTERVAL_IN_SECONDS = 60;
    private static final int DEFAULT_MAX_INIT_WAIT_TIME_IN_SECONDS = 5;
    private static final int DEFAULT_CACHE_REFRESH_INTERVAL_IN_SECONDS = 60;


    /**
     * Creates a configured auto polling configuration with default parameters.
     *
     * @return the auto polling configuration.
     */
    public static PollingMode autoPoll() {
        return new AutoPollingMode(DEFAULT_AUTO_POLL_INTERVAL_IN_SECONDS, DEFAULT_MAX_INIT_WAIT_TIME_IN_SECONDS);
    }

    /**
     * Creates a configured auto polling configuration.
     *
     * @param autoPollIntervalInSeconds Sets at least how often this policy should fetch the latest configuration and refresh the cache.
     * @return the auto polling configuration.
     */
    public static PollingMode autoPoll(int autoPollIntervalInSeconds) {
        return new AutoPollingMode(autoPollIntervalInSeconds, DEFAULT_MAX_INIT_WAIT_TIME_IN_SECONDS);
    }

    /**
     * Creates a configured auto polling configuration.
     *
     * @param autoPollIntervalInSeconds Sets at least how often this policy should fetch the latest configuration and refresh the cache.
     * @param maxInitWaitTimeSeconds    Sets the maximum waiting time between initialization and the first config acquisition in seconds.
     * @return the auto polling configuration.
     */
    public static PollingMode autoPoll(int autoPollIntervalInSeconds, int maxInitWaitTimeSeconds) {
        return new AutoPollingMode(autoPollIntervalInSeconds, maxInitWaitTimeSeconds);
    }

    /**
     * Creates a configured lazy loading polling configuration with default parameters.
     *
     * @return the lazy loading polling configuration.
     */
    public static PollingMode lazyLoad() {
        return new LazyLoadingMode(DEFAULT_CACHE_REFRESH_INTERVAL_IN_SECONDS);
    }

    /**
     * Creates a configured lazy loading polling configuration.
     *
     * @param cacheRefreshIntervalInSeconds Sets how long the cache will store its value before fetching the latest from the network again.
     * @return the lazy loading polling configuration.
     */
    public static PollingMode lazyLoad(int cacheRefreshIntervalInSeconds) {
        return new LazyLoadingMode(cacheRefreshIntervalInSeconds);
    }

    /**
     * Creates a configured manual polling configuration.
     *
     * @return the manual polling configuration.
     */
    public static PollingMode manualPoll() {
        return new ManualPollingMode();
    }
}
