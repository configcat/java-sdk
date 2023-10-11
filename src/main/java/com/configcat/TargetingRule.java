package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Targeting rule.
 */
public class TargetingRule {
    /**
     * The list of conditions (where there is a logical AND relation between the items).
     */
    @SerializedName(value = "c")
    private Condition[] conditions;
    /**
     * The list of percentage options associated with the targeting rule or {@code null} if the targeting rule has a simple value THEN part.
     */
    @SerializedName(value = "p")
    private PercentageOption[] percentageOptions;
    /**
     * The value associated with the targeting rule or {@code null} if the targeting rule has percentage options THEN part.
     */
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
