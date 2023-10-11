package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * ConfigCat segment.
 */
public class Segment {

    /**
     * The name of the segment.
     */
    @SerializedName(value = "n")
    private String name;

    /**
     * The list of segment rule conditions (where there is a logical AND relation between the items).
     */
    @SerializedName(value = "r")
    private UserCondition[] segmentRules;

    public String getName() {
        return name;
    }

    public UserCondition[] getSegmentRules() {
        return segmentRules;
    }
}
