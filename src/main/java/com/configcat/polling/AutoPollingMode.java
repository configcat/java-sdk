package com.configcat.polling;

public class AutoPollingMode implements PollingMode {
    private final int autoPollRateInSeconds;
    private final int maxInitWaitTimeSeconds;

    AutoPollingMode(int autoPollRateInSeconds, int maxInitWaitTimeSeconds) {
        if (autoPollRateInSeconds < 1)
            throw new IllegalArgumentException("autoPollRateInSeconds cannot be less than 1 second");

        this.autoPollRateInSeconds = autoPollRateInSeconds;
        this.maxInitWaitTimeSeconds = maxInitWaitTimeSeconds;
    }

    public int getAutoPollRateInSeconds() {
        return autoPollRateInSeconds;
    }

    public int getMaxInitWaitTimeSeconds() {
        return maxInitWaitTimeSeconds;
    }

    @Override
    public String getPollingIdentifier() {
        return "a";
    }
}
