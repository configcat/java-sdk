package com.configcat.polling;

/**
 * The manual polling mode configuration.
 */
public class ManualPollingMode extends PollingMode {
    @Override
    public String getPollingIdentifier() {
        return "m";
    }
}
