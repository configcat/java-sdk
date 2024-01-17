package com.configcat;

interface ConditionAccessor {
    UserCondition getUserCondition();

    SegmentCondition getSegmentCondition();

    PrerequisiteFlagCondition getPrerequisiteFlagCondition();
}
