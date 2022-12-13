package com.configcat;

class AutoPollingMode extends PollingMode {
    private final int autoPollRateInSeconds;
    private final int maxInitWaitTimeSeconds;

    AutoPollingMode(int autoPollRateInSeconds, int maxInitWaitTimeSeconds) {
        if (autoPollRateInSeconds < 1)
            throw new IllegalArgumentException("autoPollRateInSeconds cannot be less than 1 seconds");

        this.autoPollRateInSeconds = autoPollRateInSeconds;
        this.maxInitWaitTimeSeconds = maxInitWaitTimeSeconds;
    }

    int getAutoPollRateInSeconds() {
        return autoPollRateInSeconds;
    }

    public int getMaxInitWaitTimeSeconds() {
        return maxInitWaitTimeSeconds;
    }

    @Override
    String getPollingIdentifier() {
        return "a";
    }
}
