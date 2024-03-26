package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Describes a condition that is based on a prerequisite flag.
 */
public class PrerequisiteFlagCondition {

    @SerializedName(value = "f")
    private String prerequisiteFlagKey;
    @SerializedName(value = "c")
    private int prerequisiteComparator;
    @SerializedName(value = "v")
    private SettingValue value;

    /**
     * The key of the prerequisite flag that the condition is based on.
     */
    public String getPrerequisiteFlagKey() {
        return prerequisiteFlagKey;
    }

    /**
     * The operator which defines the relation between the evaluated value of the prerequisite flag and the comparison value.
     */
    public int getPrerequisiteComparator() {
        return prerequisiteComparator;
    }

    /**
     * The value that the evaluated value of the prerequisite flag is compared to.
     * Can be a value of the following types: {@link Boolean}, {@link String}, {@link Integer} or {@link Double}.
     */
    public SettingValue getValue() {
        return value;
    }
}
