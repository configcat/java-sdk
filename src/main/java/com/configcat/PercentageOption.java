package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a percentage option.
 */
public class PercentageOption {

    @SerializedName(value = "p")
    private int percentage;
    @SerializedName(value = "v")
    private SettingsValue value;
    @SerializedName(value = "i")
    private String variationId;

    /**
     * A number between 0 and 100 that represents a randomly allocated fraction of the users.
     */
    public int getPercentage() {
        return percentage;
    }

    /**
     * The value associated with the percentage option.
     * Can be a value of the following types: {@link Boolean}, {@link String}, {@link Integer} or {@link Double}.
     */
    public SettingsValue getValue() {
        return value;
    }

    /**
     * Variation ID.
     */
    public String getVariationId() {
        return variationId;
    }
}
