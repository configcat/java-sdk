package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * ConfigCat Feature Flag / Setting.
 */
public class Setting {

    /**
     * Setting type.
     */
    @SerializedName(value = "t")
    private SettingType type = SettingType.BOOLEAN;
    /**
     * The User Object attribute which serves as the basis of percentage options evaluation.
     */
    @SerializedName(value = "a")
    private String percentageAttribute;
    /**
     * The list of percentage options.
     */
    @SerializedName(value = "p")
    private PercentageOption[] percentageOptions;
    /**
     * The list of targeting rules (where there is a logical OR relation between the items).
     */
    @SerializedName(value = "r")
    private TargetingRule[] targetingRules;
    /**
     * The value of the setting.
     */
    @SerializedName(value = "v")
    private SettingsValue settingsValue;
    /**
     * The variation ID of the setting.
     */
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