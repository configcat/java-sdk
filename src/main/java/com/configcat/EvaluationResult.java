package com.configcat;

class EvaluationResult {
    public final SettingsValue value;
    public final String variationId;
    public final TargetingRule matchedTargetingRule;
    public final PercentageOption matchedPercentageOption;

    EvaluationResult(SettingsValue value, String variationId, TargetingRule matchedTargetingRule, PercentageOption matchedPercentageOption) {
        this.value = value;
        this.variationId = variationId;
        this.matchedTargetingRule = matchedTargetingRule;
        this.matchedPercentageOption = matchedPercentageOption;
    }
}