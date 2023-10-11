package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Describes the type of ConfigCat Feature Flag / Setting.
 */
public enum SettingType {
    /**
     * Represents a feature flag.
     */
    @SerializedName("0")
    BOOLEAN,

    /**
     * Represents a string setting.
     */
    @SerializedName("1")
    STRING,

    /**
     * Represents a whole number setting.
     */
    @SerializedName("2")
    INT,

    /**
     * Represents a decimal number setting.
     */
    @SerializedName("3")
    DOUBLE,
}
