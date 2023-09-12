package com.configcat;

import com.google.gson.annotations.SerializedName;

public class SegmentCondition {

    @SerializedName(value = "s")
    private int segmentIndex;

    @SerializedName(value = "c")
    private int segmentComparator;

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public int getSegmentComparator() {
        return segmentComparator;
    }


}
