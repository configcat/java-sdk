package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Setting type.
 */
public enum SettingType {
    /**
     * On/off type (feature flag).
     */
    @SerializedName("0")
    BOOLEAN,

    /**
     * Text type.
     */
    @SerializedName("1")
    STRING,

    /**
     * Whole number type.
     */
    @SerializedName("2")
    INT,

    /**
     * Decimal number type.
     */
    @SerializedName("3")
    DOUBLE,
}
