package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * ConfigCat segment.
 */
public class Segment {

    @SerializedName(value = "n")
    private String name;
    @SerializedName(value = "r")
    private UserCondition[] segmentRules;

    /**
     * The name of the segment.
     */
    public String getName() {
        return name;
    }

    /**
     * The list of segment rule conditions (where there is a logical AND relation between the items).
     */
    public UserCondition[] getSegmentRules() {
        return segmentRules;
    }
}
