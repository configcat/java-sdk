package com.configcat;

public class Entry {
    private Config config;
    private String eTag;
    private String configJson;
    private String fetchTimeRaw;

    public Config getConfig() {
        return config;
    }

    public String getETag() {
        return eTag;
    }

    public long getFetchTime() {
        return fetchTimeRaw == null || fetchTimeRaw.isEmpty() ? 0 : CacheUtils.DateTimeUtils.parseToMillis(fetchTimeRaw);
    }

    public String getConfigJson() {
        return configJson;
    }

    public String getFetchTimeRaw() {
        return fetchTimeRaw;
    }

    public Entry withFetchTime(String fetchTimeRaw) {
        return new Entry(getConfig(), getETag(), getConfigJson(), fetchTimeRaw);
    }

    public Entry(Config config, String eTag, String configJson, String fetchTimeRaw) {
        this.config = config;
        this.eTag = eTag;
        this.configJson = configJson;
        this.fetchTimeRaw = fetchTimeRaw;
    }

    boolean isEmpty() {
        return EMPTY.equals(this);
    }

    public static final Entry EMPTY = new Entry(Config.EMPTY, "", "", null);
}
