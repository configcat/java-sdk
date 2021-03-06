package com.configcat;

class AutoPollingMode extends PollingMode {
    private final int autoPollRateInSeconds;
    private final ConfigurationChangeListener listener;

    AutoPollingMode(int autoPollRateInSeconds, ConfigurationChangeListener listener) {
        if(autoPollRateInSeconds < 2)
            throw new IllegalArgumentException("autoPollRateInSeconds cannot be less than 2 seconds");

        this.autoPollRateInSeconds = autoPollRateInSeconds;
        this.listener = listener;
    }

    int getAutoPollRateInSeconds() {
        return autoPollRateInSeconds;
    }

    ConfigurationChangeListener getListener() {
        return listener;
    }

    @Override
    String getPollingIdentifier() {
        return "a";
    }
}
