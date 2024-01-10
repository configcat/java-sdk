package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a percentage option.
 */
public class PercentageOption {

    /**
     * A number between 0 and 100 that represents a randomly allocated fraction of the users.
     */
    @SerializedName(value = "p")
    private int percentage;
    /**
     * The value associated with the percentage option.
     * Can be a value of the following types: {@link Boolean}, {@link String}, {@link Integer} or {@link Double}.
     */
    @SerializedName(value = "v")
    private SettingsValue value;
    /**
     * Variation ID.
     */
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
