package com.configcat;

class EvaluationResult {
    public final SettingsValue value;
    public final String variationId;
    public final TargetingRule targetingRule;
    public final PercentageOption percentageOption;

    EvaluationResult(SettingsValue value, String variationId, TargetingRule targetingRule, PercentageOption percentageOption) {
        this.value = value;
        this.variationId = variationId;
        this.targetingRule = targetingRule;
        this.percentageOption = percentageOption;
    }
}