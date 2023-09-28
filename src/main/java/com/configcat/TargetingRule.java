package com.configcat;

import com.google.gson.annotations.SerializedName;

public class TargetingRule {
    @SerializedName(value = "c")
    private Condition[] conditions;

    @SerializedName(value = "p")
    private PercentageOption[] percentageOptions;
    @SerializedName(value = "s")
    private ServedValue servedValue;

    public Condition[] getConditions() {
        return conditions != null ? conditions : new Condition[]{};
    }

    public PercentageOption[] getPercentageOptions() {
        return percentageOptions;
    }

    public ServedValue getServedValue() {
        return servedValue;
    }
}

class ServedValue {
    @SerializedName(value = "v")
    private SettingsValue value;
    @SerializedName(value = "i")
    private String variationId;

    public SettingsValue getValue() {
        return value;
    }

    public String getVariationId() {
        return variationId;
    }
}
