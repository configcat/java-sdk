package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a condition.
 */
public class Condition implements ConditionAccessor {
    @SerializedName(value = "u")
    private UserCondition userCondition;
    @SerializedName(value = "s")
    private SegmentCondition segmentCondition;
    @SerializedName(value = "p")
    private PrerequisiteFlagCondition prerequisiteFlagCondition;

    @Override
    public UserCondition getUserCondition() {
        return userCondition;
    }

    @Override
    public SegmentCondition getSegmentCondition() {
        return segmentCondition;
    }

    @Override
    public PrerequisiteFlagCondition getPrerequisiteFlagCondition() {
        return prerequisiteFlagCondition;
    }
}
