package com.configcat;

/**
 * The interface which's implementors can used to subscribe
 * to the configuration changed event on {@link AutoPollingPolicy}.
 */
public interface ConfigurationChangeListener {

    /**
     * This method will be called when a configuration changed event fired.
     *
     * @param parser The json parser which can be used to deserialize the given configuration.
     * @param newConfiguration The new configuration json string.
     */
    void onConfigurationChanged(ConfigurationParser parser, String newConfiguration);
}
