package com.configcat;

import com.google.gson.annotations.SerializedName;

public class PrerequisiteFlagCondition {

    @SerializedName(value = "f")
    private String prerequisiteFlagKey;
    @SerializedName(value = "c")
    private int prerequisiteComparator;
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
