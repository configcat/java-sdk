package com.configcat.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class Setting {
    /**
     * Value of the feature flag / setting.
     */
    @SerializedName(value = "v")
    private JsonElement value;

    /**
     * Type of the feature flag / setting.
     */
    @SerializedName(value = "t")
    private SettingType type = SettingType.BOOLEAN;

    /**
     * Collection of percentage rules that belongs to the feature flag / setting.
     */
    @SerializedName(value = "p")
    private PercentageRule[] percentageItems;

    /**
     * Collection of targeting rules that belongs to the feature flag / setting.
     */
    @SerializedName(value = "r")
    private RolloutRule[] rolloutRules;

    /**
     * Variation ID (for analytical purposes).
     */
    @SerializedName(value = "i")
    private final String variationId = "";

    public void setValue(JsonElement value) {
        this.value = value;
    }

    public void setType(SettingType type) {
        this.type = type;
    }

    public JsonElement getValue() {
        return value;
    }

    public SettingType getType() {
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