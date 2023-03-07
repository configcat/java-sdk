package com.configcat;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

class Entry {
    @SerializedName(value = "c")
    private Config config;
    @SerializedName(value = "e")
    private String eTag;
    @SerializedName(value = "f")
    private long fetchTime;

    public Config getConfig() {
        return config;
    }

    public String getETag() {
        return eTag;
    }

    public long getFetchTime() {
        return fetchTime;
    }

    public Entry withFetchTime(long fetchTime) {
        return new Entry(getConfig(), getETag(), fetchTime);
    }

    public Entry(Config config, String eTag, long fetchTime) {
        this.config = config;
        this.eTag = eTag;
        this.fetchTime = fetchTime;
    }

    boolean isEmpty() {
        return EMPTY.equals(this);
    }

    public static final Entry EMPTY = new Entry(Config.EMPTY, "", 0);

}

class Config {
    @SerializedName(value = "p")
    private Preferences preferences;
    @SerializedName(value = "f")
    private Map<String, Setting> entries = new HashMap<>();

    public Preferences getPreferences() {
        return preferences;
    }

    public Map<String, Setting> getEntries() {
        return entries;
    }

    boolean isEmpty() {
        return EMPTY.equals(this);
    }

    public static final Config EMPTY = new Config();
}

class Preferences {
    @SerializedName(value = "u")
    private String baseUrl;
    @SerializedName(value = "r")
    private int redirect;

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getRedirect() {
        return redirect;
    }
}

