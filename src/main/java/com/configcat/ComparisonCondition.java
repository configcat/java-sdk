package com.configcat;

import com.google.gson.annotations.SerializedName;

public class ComparisonCondition {

    @SerializedName(value = "a")
    private String comparisonAttribute;

    @SerializedName(value = "c")
    private int comparator;

    @SerializedName("s")
    private String stringValue;

    @SerializedName("d")
    private Double doubleValue;

    @SerializedName("l")
    private String[] stringArrayValue;

    public String getComparisonAttribute() {
        return comparisonAttribute;
    }

    public int getComparator() {
        return comparator;
    }

    public String getStringValue() {
        return stringValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public String[] getStringArrayValue() {
        return stringArrayValue;
    }
}
