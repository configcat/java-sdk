package com.configcat;

import com.google.gson.annotations.SerializedName;

/**
 * Feature flag or setting.
 */
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

    /**
     * Setting type.
     */
    public SettingType getType() {
        return type;
    }

    /**
     * The User Object attribute which serves as the basis of percentage options evaluation.
     */
    public String getPercentageAttribute() {
        return percentageAttribute;
    }

    /**
     * The list of percentage options.
     */
    public PercentageOption[] getPercentageOptions() {
        return percentageOptions;
    }

    /**
     * The list of targeting rules (where there is a logical OR relation between the items).
     */
    public TargetingRule[] getTargetingRules() {
        return targetingRules;
    }

    /**
     * Setting value.
     * Can be a value of the following types: {@link Boolean}, {@link String}, {@link Integer} or {@link Double}.
     */
    public SettingsValue getSettingsValue() {
        return settingsValue;
    }

    /**
     * Variation ID.
     */
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