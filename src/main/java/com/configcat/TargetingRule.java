package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Describes a targeting rule.
 */
public class TargetingRule {
    /**
     * The list of conditions that are combined with the AND logical operator.
     * Items can be one of the following types: {@link UserCondition}, {@link SegmentCondition} or {@link PrerequisiteFlagCondition}.
     */
    @SerializedName(value = "c")
    private Condition[] conditions;
    /**
     * The list of percentage options associated with the targeting rule or {@code null} if the targeting rule has a simple value THEN part.
     */
    @SerializedName(value = "p")
    private PercentageOption[] percentageOptions;
    /**
     * The simple value associated with the targeting rule or {@code null} if the targeting rule has percentage options THEN part.
     */
    @SerializedName(value = "s")
    private SimpleValue simpleValue;

    public Condition[] getConditions() {
        return conditions != null ? conditions : new Condition[]{};
    }

    public PercentageOption[] getPercentageOptions() {
        return percentageOptions;
    }

    public SimpleValue getSimpleValue() {
        return simpleValue;
    }
}

class SimpleValue {
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
