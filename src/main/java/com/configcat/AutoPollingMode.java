package com.configcat;

class AutoPollingMode extends PollingMode {
    private final int autoPollRateInSeconds;
    private final ConfigurationChangeListener listener;
    private final int maxInitWaitTimeSeconds;

    AutoPollingMode(int autoPollRateInSeconds, int maxInitWaitTimeSeconds, ConfigurationChangeListener listener) {
        if (autoPollRateInSeconds < 1)
            throw new IllegalArgumentException("autoPollRateInSeconds cannot be less than 1 seconds");

        this.autoPollRateInSeconds = autoPollRateInSeconds;
        this.maxInitWaitTimeSeconds = maxInitWaitTimeSeconds;
        this.listener = listener;
    }

    int getAutoPollRateInSeconds() {
        return autoPollRateInSeconds;
    }

    public int getMaxInitWaitTimeSeconds() {
        return maxInitWaitTimeSeconds;
    }

    ConfigurationChangeListener getListener() {
        return listener;
    }

    @Override
    String getPollingIdentifier() {
        return "a";
    }
}
