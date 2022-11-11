package com.configcat;

/**
 * Describes the polling modes.
 */
public final class PollingModes {
    /**
     * Creates a configured auto polling configuration.
     *
     * @param autoPollIntervalInSeconds Sets at least how often this policy should fetch the latest configuration and refresh the cache.
     * @return the auto polling configuration.
     */
    public static PollingMode autoPoll(int autoPollIntervalInSeconds) {
        return new AutoPollingMode(autoPollIntervalInSeconds, 5, null);
    }

    /**
     * Creates a configured auto polling configuration.
     *
     * @param autoPollIntervalInSeconds Sets at least how often this policy should fetch the latest configuration and refresh the cache.
     * @param maxInitWaitTimeSeconds    Sets the maximum waiting time between initialization and the first config acquisition in seconds.
     * @return the auto polling configuration.
     */
    public static PollingMode autoPoll(int autoPollIntervalInSeconds, int maxInitWaitTimeSeconds) {
        return new AutoPollingMode(autoPollIntervalInSeconds, maxInitWaitTimeSeconds, null);
    }

    /**
     * Creates a configured auto polling configuration.
     *
     * @param autoPollIntervalInSeconds Sets at least how often this policy should fetch the latest configuration and refresh the cache.
     * @param listener                  Sets a configuration changed listener.
     * @return the auto polling configuration.
     */
    public static PollingMode autoPoll(int autoPollIntervalInSeconds, ConfigurationChangeListener listener) {
        return new AutoPollingMode(autoPollIntervalInSeconds, 5, listener);
    }

    /**
     * Creates a configured auto polling configuration.
     *
     * @param autoPollIntervalInSeconds Sets at least how often this policy should fetch the latest configuration and refresh the cache.
     * @param maxInitWaitTimeSeconds    Sets the maximum waiting time between initialization and the first config acquisition in seconds.
     * @param listener                  Sets a configuration changed listener.
     * @return the auto polling configuration.
     */
    public static PollingMode autoPoll(int autoPollIntervalInSeconds, int maxInitWaitTimeSeconds, ConfigurationChangeListener listener) {
        return new AutoPollingMode(autoPollIntervalInSeconds, maxInitWaitTimeSeconds, listener);
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
