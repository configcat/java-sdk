package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Describes a condition that is based on a User Object attribute.
 */
public class UserCondition implements ConditionAccessor {

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

    /**
     * The User Object attribute that the condition is based on. Can be "Identifier", "Email", "Country" or any custom attribute.
     */
    public String getComparisonAttribute() {
        return comparisonAttribute;
    }

    /**
     * The operator which defines the relation between the comparison attribute and the comparison value.
     */
    public int getComparator() {
        return comparator;
    }

    /**
     * The String value that the User Object attribute is compared or {@code null} if the comparator use a different type of value.
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * The Double value that the User Object attribute is compared or {@code null} if the comparator use a different type of value.
     */
    public Double getDoubleValue() {
        return doubleValue;
    }
    
    /**
     * The String Array value that the User Object attribute is compared or {@code null} if the comparator use a different type of value.
     */
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
