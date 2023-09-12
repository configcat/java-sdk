package com.configcat;

import com.google.gson.annotations.SerializedName;

public class Condition {

    @SerializedName(value = "t")
    private UserCondition userCondition;
    @SerializedName(value = "s")
    private SegmentCondition segmentCondition;
    @SerializedName(value = "d")
    private PrerequisiteFlagCondition prerequisiteFlagCondition;

    public UserCondition getComparisonCondition() {
        return userCondition;
    }

    public PrerequisiteFlagCondition getPrerequisiteFlagCondition() {
        return prerequisiteFlagCondition;
    }

    public SegmentCondition getSegmentCondition() {
        return segmentCondition;
    }
}
