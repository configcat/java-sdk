package com.configcat;

import com.google.gson.annotations.SerializedName;

public class Segment {

    @SerializedName(value = "n")
    private String name;

    @SerializedName(value = "r")
    private UserCondition[] segmentRules;

    public String getName() {
        return name;
    }

    public UserCondition[] getSegmentRules() {
        return segmentRules;
    }
}
