package com.configcat;

import com.google.gson.annotations.SerializedName;

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
}

