package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Describes a condition that is based on a User Object attribute.
 */
public class UserCondition implements ConditionAccessor {

    /**
     * The User Object attribute that the condition is based on. Can be "Identifier", "Email", "Country" or any custom attribute.
     */
    @SerializedName(value = "a")
    private String comparisonAttribute;

    /**
     * The operator which defines the relation between the comparison attribute and the comparison value.
     */
    @SerializedName(value = "c")
    private int comparator;
    /**
     * The String value that the User Object attribute is compared or {@code null} if the comparator use a different type of value.
     */
    @SerializedName("s")
    private String stringValue;
    /**
     * The Double value that the User Object attribute is compared or {@code null} if the comparator use a different type of value.
     */
    @SerializedName("d")
    private Double doubleValue;
    /**
     * The String Array value that the User Object attribute is compared or {@code null} if the comparator use a different type of value.
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

    @Override
    public UserCondition getUserCondition() {
        return this;
    }

    @Override
    public SegmentCondition getSegmentCondition() {
        return null;
    }

    @Override
    public PrerequisiteFlagCondition getPrerequisiteFlagCondition() {
        return null;
    }
}
