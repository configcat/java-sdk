package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Percentage option.
 */
public class PercentageOption {

    /**
     * A number between 0 and 100 that represents a randomly allocated fraction of the users.
     */
    @SerializedName(value = "p")
    private int percentage;
    /**
     * The server value of the percentage option.
     */
    @SerializedName(value = "v")
    private SettingsValue value;
    /**
     * The variation ID of the percentage option.
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
