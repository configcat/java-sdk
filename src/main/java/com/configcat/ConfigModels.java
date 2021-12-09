package com.configcat;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

class Config {
    public transient String JsonString;

    @SerializedName(value = "p")
    public Preferences Preferences;
    @SerializedName(value = "f")
    public Map<String, Setting> Entries;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        return ((Config)obj).JsonString.equals(this.JsonString);
    }

    @Override
    public int hashCode() {
        return this.JsonString.hashCode();
    }
}

class Preferences {
    @SerializedName(value = "u")
    public String BaseUrl;
    @SerializedName(value = "r")
    public int Redirect;
}

class Setting {
    @SerializedName(value = "v")
    public JsonElement Value;
    @SerializedName(value = "t")
    public String Type;
    @SerializedName(value = "p")
    public RolloutPercentageItem[] RolloutPercentageItems;
    @SerializedName(value = "r")
    public RolloutRule[] RolloutRules;
    @SerializedName(value = "i")
    public String VariationId = "";
}

class RolloutRule {
    @SerializedName(value = "v")
    public JsonElement Value;
    @SerializedName(value = "a")
    public String ComparisonAttribute;
    @SerializedName(value = "t")
    public int Comparator;
    @SerializedName(value = "c")
    public String ComparisonValue;
    @SerializedName(value = "i")
    public String VariationId;
}

class RolloutPercentageItem {
    @SerializedName(value = "v")
    public JsonElement Value;
    @SerializedName(value = "p")
    public double Percentage;
    @SerializedName(value = "i")
    public String VariationId;
}
