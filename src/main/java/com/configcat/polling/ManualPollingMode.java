package com.configcat.polling;

/**
 * The manual polling mode configuration.
 */
public class ManualPollingMode implements PollingMode {
    @Override
    public String getPollingIdentifier() {
        return "m";
    }
}
