package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Describes a condition that is based on a segment.
 */
public class SegmentCondition {

    @SerializedName(value = "s")
    private int segmentIndex;

    @SerializedName(value = "c")
    private int segmentComparator;

    /**
     * The index of the segment that the condition is based on.
     */
    public int getSegmentIndex() {
        return segmentIndex;
    }

    /**
     * The operator which defines the expected result of the evaluation of the segment.
     */
    public int getSegmentComparator() {
        return segmentComparator;
    }


}
