package com.configcat;

/**
 * The interface for the data classes which can be passed to the RolloutEvaluator.evaluateConditions method.
 */
public interface ConditionAccessor {
    UserCondition getUserCondition();

    SegmentCondition getSegmentCondition();

    PrerequisiteFlagCondition getPrerequisiteFlagCondition();
}
