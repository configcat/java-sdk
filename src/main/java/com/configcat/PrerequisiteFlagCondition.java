package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Prerequisite Flag Condition.
 */
public class PrerequisiteFlagCondition {
    /**
     * The key of the prerequisite flag that the condition is based on.
     */
    @SerializedName(value = "f")
    private String prerequisiteFlagKey;
    /**
     * The operator which defines the relation between the evaluated value of the prerequisite flag and the comparison value.
     */
    @SerializedName(value = "c")
    private int prerequisiteComparator;
    /**
     * The value that the evaluated value of the prerequisite flag is compared to.
     */
    @SerializedName(value = "v")
    private SettingsValue value;

    public String getPrerequisiteFlagKey() {
        return prerequisiteFlagKey;
    }

    public int getPrerequisiteComparator() {
        return prerequisiteComparator;
    }

    public SettingsValue getValue() {
        return value;
    }
}
