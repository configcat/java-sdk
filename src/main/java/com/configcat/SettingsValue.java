package com.configcat;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class SettingsValue {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettingsValue that = (SettingsValue) o;
        return Objects.equals(booleanValue, that.booleanValue) && Objects.equals(stringValue, that.stringValue) && Objects.equals(integerValue, that.integerValue) && Objects.equals(doubleValue, that.doubleValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValue, stringValue, integerValue, doubleValue);
    }
}

