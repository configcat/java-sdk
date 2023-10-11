package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * User Condition.
 */
public class UserCondition {

    /**
     * The User Object attribute that the condition is based on. Can be "User ID", "Email", "Country" or any custom attribute.
     */
    @SerializedName(value = "a")
    private String comparisonAttribute;

    /**
     * The operator which defines the relation between the comparison attribute and the comparison value.
     */
    @SerializedName(value = "c")
    private int comparator;
    /**
     * The String value that the attribute is compared or {@code null} if the comparator use a different type.
     */
    @SerializedName("s")
    private String stringValue;
    /**
     * The Double value that the attribute is compared or {@code null} if the comparator use a different type.
     */
    @SerializedName("d")
    private Double doubleValue;
    /**
     * The String Array value that the attribute is compared or {@code null} if the comparator use a different type.
     */
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
