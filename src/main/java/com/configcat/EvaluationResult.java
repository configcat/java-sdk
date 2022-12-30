package com.configcat;

import com.google.gson.JsonElement;

class EvaluationResult {
    public final JsonElement value;
    public final String variationId;
    public final RolloutRule targetingRule;
    public final PercentageRule percentageRule;

    EvaluationResult(JsonElement value, String variationId, RolloutRule targetingRule, PercentageRule percentageRule) {
        this.value = value;
        this.variationId = variationId;
        this.targetingRule = targetingRule;
        this.percentageRule = percentageRule;
    }
}