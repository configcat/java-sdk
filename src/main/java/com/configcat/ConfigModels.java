package com.configcat;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

enum SettingType {
    BOOLEAN,
    STRING,
    INT,
    DOUBLE,
}

class Entry {
    @SerializedName(value = "c")
    public Config config;
    @SerializedName(value = "e")
    public String eTag;
    @SerializedName(value = "f")
    public long fetchTime;

    public Entry(Config config, String eTag, long fetchTime) {
        this.config = config;
        this.eTag = eTag;
        this.fetchTime = fetchTime;
    }

    boolean isEmpty() {
        return this == empty;
    }

    public static final Entry empty = new Entry(Config.empty, "", 0);
}

class Config {
    @SerializedName(value = "p")
    public Preferences preferences;
    @SerializedName(value = "f")
    public Map<String, Setting> entries = new HashMap<>();

    public static Config empty = new Config();
}

class Preferences {
    @SerializedName(value = "u")
    public String baseUrl;
    @SerializedName(value = "r")
    public int redirect;
}

class Setting {
    @SerializedName(value = "v")
    public JsonElement value;
    @SerializedName(value = "t")
    public int type;
    @SerializedName(value = "p")
    public PercentageRule[] percentageItems;
    @SerializedName(value = "r")
    public RolloutRule[] rolloutRules;
    @SerializedName(value = "i")
    public String variationId = "";
}

class RolloutRule {
    @SerializedName(value = "v")
    public JsonElement value;
    @SerializedName(value = "a")
    public String comparisonAttribute;
    @SerializedName(value = "t")
    public int comparator;
    @SerializedName(value = "c")
    public String comparisonValue;
    @SerializedName(value = "i")
    public String variationId;
}

class PercentageRule {
    @SerializedName(value = "v")
    public JsonElement value;
    @SerializedName(value = "p")
    public double percentage;
    @SerializedName(value = "i")
    public String variationId;
}
