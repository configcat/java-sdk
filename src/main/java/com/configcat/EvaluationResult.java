package com.configcat;

class EvaluationResult {
    public final SettingValue value;
    public final String variationId;
    public final TargetingRule matchedTargetingRule;
    public final PercentageOption matchedPercentageOption;

    EvaluationResult(SettingValue value, String variationId, TargetingRule matchedTargetingRule, PercentageOption matchedPercentageOption) {
        this.value = value;
        this.variationId = variationId;
        this.matchedTargetingRule = matchedTargetingRule;
        this.matchedPercentageOption = matchedPercentageOption;
    }
}