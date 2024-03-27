package com.configcat;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Describes the setting type-specific value of a setting or feature flag.
 */
public class SettingValue {
    @SerializedName("b")
    private Boolean booleanValue;

    @SerializedName("s")
    private String stringValue;

    @SerializedName("i")
    private Integer integerValue;

    @SerializedName("d")
    private Double doubleValue;

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public boolean equalsBasedOnSettingType(Object o, SettingType settingType) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettingValue that = (SettingValue) o;
        switch (settingType){
            case BOOLEAN:
                return Objects.equals(booleanValue, that.booleanValue);
            case STRING:
                return Objects.equals(stringValue, that.stringValue);
            case INT:
                return Objects.equals(integerValue, that.integerValue);
            case DOUBLE:
                return Objects.equals(doubleValue, that.doubleValue);
            default:
                throw new IllegalStateException("Setting is of an unsupported type (" + settingType.name() + ").");
        }
    }

    @Override
    public String toString() {
        if (booleanValue != null) {
            return booleanValue.toString();
        } else if (integerValue != null) {
            return integerValue.toString();
        } else if (doubleValue != null) {
            return doubleValue.toString();
        } else {
            return stringValue;
        }
    }
}

