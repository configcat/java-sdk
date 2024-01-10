package com.configcat;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Details of a ConfigCat config.
 */
public class Config {

    /**
     * The config preferences.
     */
    @SerializedName(value = "p")
    private Preferences preferences;
    /**
     * The map of settings.
     */
    @SerializedName(value = "f")
    private final Map<String, Setting> entries = new HashMap<>();

    /**
     * The list of segments.
     */
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