package com.configcat;

import com.google.gson.annotations.SerializedName;

public class SegmentCondition {

    @SerializedName(value = "s")
    private Double segmentIndex;

    @SerializedName(value = "c")
    private int segmentComparator;

    public Double getSegmentIndex() {
        return segmentIndex;
    }

    public int getSegmentComparator() {
        return segmentComparator;
    }
}
