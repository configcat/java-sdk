package com.configcat;

import java.util.Map;

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
        return new LazyLoadingMode(cacheRefreshIntervalInSeconds, false);
    }

    /**
     * Creates a configured lazy loading polling configuration.
     *
     * @param cacheRefreshIntervalInSeconds Sets how long the cache will store its value before fetching the latest from the network again.
     * @param asyncRefresh                  Sets whether the cache should refresh itself asynchronously or synchronously.
     *                                      <p>If it's set to {@code true} reading from the policy will not wait for the refresh to be finished,
     *                                      instead it returns immediately with the previous stored value.</p>
     *                                      <p>If it's set to {@code false} the policy will wait until the expired
     *                                      value is being refreshed with the latest configuration.</p>
     * @return the lazy loading polling configuration.
     */
    public static PollingMode lazyLoad(int cacheRefreshIntervalInSeconds, boolean asyncRefresh) {
        return new LazyLoadingMode(cacheRefreshIntervalInSeconds, asyncRefresh);
    }

    /**
     * Creates a configured manual polling configuration.
     *
     * @return the manual polling configuration.
     */
    public static PollingMode manualPoll() {
        return new ManualPollingMode();
    }

    /**
     * Creates a local file polling configuration.
     *
     * @return the local file polling configuration.
     */
    public static PollingMode localFile(String filePath, boolean autoReload) {
        return new LocalFilePollingMode(filePath, autoReload, false);
    }

    /**
     * Creates a local classpath resource file polling configuration.
     *
     * @return the local file polling configuration.
     */
    public static PollingMode localClassPathResource(String filePath, boolean autoReload) {
        return new LocalFilePollingMode(filePath, autoReload, true);
    }

    /**
     * Creates a polling configuration that produces the internal flags & settings structure from a {@code Map<String, Object>}.
     *
     * @return the object polling configuration.
     */
    public static PollingMode localObject(Map<String, Object> source) {
        return new LocalMapPollingMode(source);
    }
}
