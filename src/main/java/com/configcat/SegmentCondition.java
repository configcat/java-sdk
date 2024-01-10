package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Describes a condition that is based on a segment.
 */
public class SegmentCondition {
    /**
     * The index of the segment that the condition is based on.
     */
    @SerializedName(value = "s")
    private int segmentIndex;

    /**
     * The operator which defines the expected result of the evaluation of the segment.
     */
    @SerializedName(value = "c")
    private int segmentComparator;

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public int getSegmentComparator() {
        return segmentComparator;
    }


}
