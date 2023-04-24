package com.configcat;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

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

