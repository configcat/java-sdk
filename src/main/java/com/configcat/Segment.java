package com.configcat;

import com.google.gson.annotations.SerializedName;

public class Segment {

    @SerializedName(value = "n")
    private String name;

    @SerializedName(value = "r")
    private ComparisonCondition[] segmentRules;

    public String getName() {
        return name;
    }

    public ComparisonCondition[] getSegmentRules() {
        return segmentRules;
    }
}
