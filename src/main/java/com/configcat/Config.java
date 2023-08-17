package com.configcat;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class Config {

    @SerializedName(value = "p")
    private Preferences preferences;
    @SerializedName(value = "f")
    private Map<String, Setting> entries = new HashMap<>();

    @SerializedName(value = "s")
    private Segment[] segments;

    public Preferences getPreferences() {
        return preferences;
    }

    public Segment[] getSegments() {
        return segments;
    }

    public Map<String, Setting> getEntries() {
        return entries;
    }

    boolean isEmpty() {
        return EMPTY.equals(this);
    }

    public static final Config EMPTY = new Config();
}