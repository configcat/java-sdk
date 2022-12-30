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

class Setting {
    @SerializedName(value = "v")
    private JsonElement value;
    @SerializedName(value = "t")
    private int type;
    @SerializedName(value = "p")
    private PercentageRule[] percentageItems;
    @SerializedName(value = "r")
    private RolloutRule[] rolloutRules;
    @SerializedName(value = "i")
    private String variationId = "";

    public void setValue(JsonElement value) {
        this.value = value;
    }

    public JsonElement getValue() {
        return value;
    }

    public int getType() {
        return type;
    }

    public PercentageRule[] getPercentageItems() {
        return percentageItems;
    }

    public RolloutRule[] getRolloutRules() {
        return rolloutRules;
    }

    public String getVariationId() {
        return variationId;
    }
}

class RolloutRule {
    @SerializedName(value = "v")
    private JsonElement value;
    @SerializedName(value = "a")
    private String comparisonAttribute;
    @SerializedName(value = "t")
    private int comparator;
    @SerializedName(value = "c")
    private String comparisonValue;
    @SerializedName(value = "i")
    private String variationId;

    public JsonElement getValue() {
        return value;
    }

    public String getComparisonAttribute() {
        return comparisonAttribute;
    }

    public int getComparator() {
        return comparator;
    }

    public String getComparisonValue() {
        return comparisonValue;
    }

    public String getVariationId() {
        return variationId;
    }
}

class PercentageRule {
    @SerializedName(value = "v")
    private JsonElement value;
    @SerializedName(value = "p")
    private double percentage;
    @SerializedName(value = "i")
    private String variationId;

    public JsonElement getValue() {
        return value;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getVariationId() {
        return variationId;
    }
}
