package com.configcat;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Details of a ConfigCat config.
 */
public class Config {

    @SerializedName(value = "p")
    private Preferences preferences;
    @SerializedName(value = "f")
    private final Map<String, Setting> entries = new HashMap<>();
    @SerializedName(value = "s")
    private Segment[] segments;

    /**
     * The config preferences.
     */
    public Preferences getPreferences() {
        return preferences;
    }

    /**
     * The list of segments.
     */
    public Segment[] getSegments() {
        return segments;
    }

    /**
     * The map of settings.
     */
    public Map<String, Setting> getEntries() {
        return entries;
    }

    boolean isEmpty() {
        return EMPTY.equals(this);
    }

    public static final Config EMPTY = new Config();
}