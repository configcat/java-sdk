package com.configcat;

import com.google.gson.annotations.SerializedName;

public class PercentageOption {

    @SerializedName(value = "p")
    private int percentage;
    @SerializedName(value = "v")
    private SettingsValue value;
    @SerializedName(value = "i")
    private String variationId;

    public int getPercentage() {
        return percentage;
    }

    public SettingsValue getValue() {
        return value;
    }

    public String getVariationId() {
        return variationId;
    }
}
