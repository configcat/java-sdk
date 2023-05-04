package com.configcat.polling;

/**
 * The base class of a polling mode configuration.
 */
public interface PollingMode {
    default String getPollingIdentifier() {
        return "n/a";
    }
}
