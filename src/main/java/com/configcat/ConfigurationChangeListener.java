package com.configcat;

/**
 * The interface which's implementors can be used to subscribe
 * to the configuration changed event on {@link ConfigService} with {@link AutoPollingMode}.
 */
public interface ConfigurationChangeListener {

    /**
     * This method will be called when a configuration changed event fired.
     */
    void onConfigurationChanged();
}
