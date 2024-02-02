package com.configcat;

public class Entry {
    private final Config config;
    private final String eTag;
    private final String configJson;
    private final long fetchTime;

    public Config getConfig() {
        return config;
    }

    public String getETag() {
        return eTag;
    }

    public long getFetchTime() {
        return fetchTime;
    }

    public String getConfigJson() {
        return configJson;
    }

    public Entry withFetchTime(long fetchTime) {
        return new Entry(getConfig(), getETag(), getConfigJson(), fetchTime);
    }

    public Entry(Config config, String eTag, String configJson, long fetchTime) {
        this.config = config;
        this.eTag = eTag;
        this.configJson = configJson;
        this.fetchTime = fetchTime;
    }

    boolean isEmpty() {
        return EMPTY.equals(this);
    }

    public static final Entry EMPTY = new Entry(Config.EMPTY, "", "", Constants.DISTANT_PAST);

    public String serialize() {
        return getFetchTime() + "\n" + getETag() + "\n" + getConfigJson();
    }

    public static Entry fromString(String cacheValue) throws IllegalArgumentException {
        if (cacheValue == null || cacheValue.isEmpty()) {
            return Entry.EMPTY;
        }

        int fetchTimeIndex = cacheValue.indexOf("\n");
        int eTagIndex = cacheValue.indexOf("\n", fetchTimeIndex + 1);
        if (fetchTimeIndex < 0 || eTagIndex < 0) {
            throw new IllegalArgumentException("Number of values is fewer than expected.");
        }
        String fetchTimeRaw = cacheValue.substring(0, fetchTimeIndex);
        if (!DateTimeUtils.isValidDate(fetchTimeRaw)) {
            throw new IllegalArgumentException("Invalid fetch time: " + fetchTimeRaw);
        }
        long fetchTimeUnixMillis = Long.parseLong(fetchTimeRaw);


        String eTag = cacheValue.substring(fetchTimeIndex + 1, eTagIndex);
        if (eTag.isEmpty()) {
            throw new IllegalArgumentException("Empty eTag value.");
        }
        String configJson = cacheValue.substring(eTagIndex + 1);
        if (configJson.isEmpty()) {
            throw new IllegalArgumentException("Empty config jsom value.");
        }
        try {
            Config config = Utils.deserializeConfig(configJson);
            return new Entry(config, eTag, configJson, fetchTimeUnixMillis);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid config JSON content: " + configJson);
        }
    }
}
