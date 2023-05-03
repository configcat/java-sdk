package com.configcat.evaluation;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * Describes a rollout percentage rule.
 */
public class PercentageRule {
    /**
     * Value served when the rule is selected during evaluation.
     */
    @SerializedName(value = "v")
    private JsonElement value;

    /**
     * The rule's percentage value.
     */
    @SerializedName(value = "p")
    private double percentage;

    /**
     * The rule's variation ID (for analytical purposes).
     */
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