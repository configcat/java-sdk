package com.configcat;

import com.google.gson.annotations.SerializedName;

public class Setting {

    @SerializedName(value = "t")
    private SettingType type = SettingType.BOOLEAN;
    @SerializedName(value = "a")
    private String percentageAttribute;
    @SerializedName(value = "p")
    private PercentageOption[] percentageOptions;
    @SerializedName(value = "r")
    private TargetingRule[] targetingRules;
    @SerializedName(value = "v")
    private SettingsValue settingsValue;
    @SerializedName(value = "i")
    private String variationId;

    private String configSalt;
    private Segment[] segments;

    public void setSettingsValue(SettingsValue settingsValue) {
        this.settingsValue = settingsValue;
    }

    public void setType(SettingType type) {
        this.type = type;
    }

    public void setConfigSalt(String configSalt) {
        this.configSalt = configSalt;
    }

    public void setSegments(Segment[] segments) {
        this.segments = segments;
    }

    public SettingType getType() {
        return type;
    }

    public String getPercentageAttribute() {
        return percentageAttribute;
    }

    public PercentageOption[] getPercentageOptions() {
        return percentageOptions;
    }

    public TargetingRule[] getTargetingRules() {
        return targetingRules;
    }

    public SettingsValue getSettingsValue() {
        return settingsValue;
    }

    public String getVariationId() {
        return variationId;
    }

    public String getConfigSalt() {
        return configSalt;
    }

    public Segment[] getSegments() {
        return segments;
    }
}