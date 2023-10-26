package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Container class for different condition types.
 */
public class Condition {
    @SerializedName(value = "u")
    private UserCondition userCondition;
    @SerializedName(value = "s")
    private SegmentCondition segmentCondition;
    @SerializedName(value = "p")
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
