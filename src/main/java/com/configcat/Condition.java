package com.configcat;

import com.google.gson.annotations.SerializedName;

public class Condition {

    @SerializedName(value = "t")
    private ComparisonCondition comparisonCondition;
    @SerializedName(value = "s")
    private SegmentCondition segmentCondition;
    @SerializedName(value = "d")
    private PrerequisiteFlagCondition prerequisiteFlagCondition;

    public ComparisonCondition getComparisonCondition() {
        return comparisonCondition;
    }

    public PrerequisiteFlagCondition getPrerequisiteFlagCondition() {
        return prerequisiteFlagCondition;
    }

    public SegmentCondition getSegmentCondition() {
        return segmentCondition;
    }
}
