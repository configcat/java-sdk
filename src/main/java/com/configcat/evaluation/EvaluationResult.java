package com.configcat.evaluation;

import com.configcat.models.PercentageRule;
import com.configcat.models.RolloutRule;
import com.google.gson.JsonElement;

public class EvaluationResult {
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