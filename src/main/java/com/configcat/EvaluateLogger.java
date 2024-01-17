package com.configcat;

public class EvaluateLogger {

    private final StringBuilder stringBuilder = new StringBuilder();

    public EvaluateLogger(LogLevel logLevel) {
        indentLevel = 0;
        isLoggable = logLevel.ordinal() <= LogLevel.INFO.ordinal();
    }

    private int indentLevel;

    private final boolean isLoggable;

    public final boolean isLoggable() {
        return isLoggable;
    }

    public final void logUserObject(final User user) {
        if (!isLoggable) {
            return;
        }
        append(" for User '" + user.toString() + "'");
    }

    public final void logEvaluation(String key) {
        if (!isLoggable) {
            return;
        }
        append("Evaluating '" + key + "'");
    }

    public final void logPercentageOptionUserMissing() {
        if (!isLoggable) {
            return;
        }
        newLine();
        append("Skipping % options because the User Object is missing.");
    }

    public final void logPercentageOptionUserAttributeMissing(String percentageOptionsAttributeName) {
        if (!isLoggable) {
            return;
        }
        newLine();
        append("Skipping % options because the User." + percentageOptionsAttributeName + " attribute is missing.");
    }

    public final void logPercentageOptionEvaluation(String percentageOptionsAttributeName) {
        if (!isLoggable) {
            return;
        }
        newLine();
        append("Evaluating % options based on the User." + percentageOptionsAttributeName + " attribute:");
    }

    public final void logPercentageOptionEvaluationHash(String percentageOptionsAttributeName, int hashValue) {
        if (!isLoggable) {
            return;
        }
        newLine();
        append("- Computing hash in the [0..99] range from User." + percentageOptionsAttributeName + " => " + hashValue + " (this value is sticky and consistent across all SDKs)");
    }

    public final void append(final String line) {
        if (!isLoggable) {
            return;
        }
        stringBuilder.append(line);
    }

    public final void increaseIndentLevel() {
        if (!isLoggable) {
            return;
        }
        indentLevel++;
    }

    public final void decreaseIndentLevel() {
        if (!isLoggable) {
            return;
        }
        if (indentLevel > 0) {
            indentLevel--;
        }
    }

    public final void newLine() {
        if (!isLoggable) {
            return;
        }
        stringBuilder.append("\n");
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append("  ");
        }
    }

    public String toPrint() {
        if (!isLoggable) {
            return "";
        }
        return stringBuilder.toString();
    }

    public void logReturnValue(String returnValue) {
        if (!isLoggable) {
            return;
        }
        newLine();
        append("Returning '" + returnValue + "'.");
    }

    public void logTargetingRules() {
        if (!isLoggable) {
            return;
        }
        newLine();
        append("Evaluating targeting rules and applying the first match if any:");
    }

    public void logConditionConsequence(boolean result) {
        if (!isLoggable) {
            return;
        }
        append(" => " + result);
        if (!result) {
            append(", skipping the remaining AND conditions");
        }
    }

    public void logTargetingRuleIgnored() {
        if (!isLoggable) {
            return;
        }
        increaseIndentLevel();
        newLine();
        append("The current targeting rule is ignored and the evaluation continues with the next rule.");
        decreaseIndentLevel();
    }

    public void logTargetingRuleConsequence(TargetingRule targetingRule, String error, boolean isMatch, boolean newLine) {
        if (!isLoggable) {
            return;
        }
        increaseIndentLevel();
        String valueFormat = "% options";
        if (targetingRule != null && targetingRule.getSimpleValue() != null && targetingRule.getSimpleValue().getValue() != null) {
            valueFormat = "'" + targetingRule.getSimpleValue().getValue() + "'";
        }
        if (newLine) {
            newLine();
        } else {
            append(" ");
        }
        append("THEN " + valueFormat + " => ");
        if (error != null && !error.isEmpty()) {
            append(error);
        } else {
            if (isMatch) {
                append("MATCH, applying rule");
            } else {
                append("no match");
            }
        }
        decreaseIndentLevel();
    }

    public void logPercentageEvaluationReturnValue(int hashValue, int i, int percentage, SettingsValue settingsValue) {
        if (!isLoggable) {
            return;
        }
        String percentageOptionValue = settingsValue != null ? settingsValue.toString() : LogHelper.INVALID_VALUE;
        newLine();
        append("- Hash value " + hashValue + " selects % option " + (i + 1) + " (" + percentage + "%), '" + percentageOptionValue + "'.");
    }

    public void logSegmentEvaluationStart(String segmentName) {
        if (!isLoggable) {
            return;
        }
        newLine();
        append("(");
        increaseIndentLevel();
        newLine();
        append("Evaluating segment '" + segmentName + "':");
    }

    public void logSegmentEvaluationResult(SegmentCondition segmentCondition, Segment segment, boolean result, boolean segmentResult) {
        if (!isLoggable) {
            return;
        }
        newLine();
        String segmentResultComparator = segmentResult ? SegmentComparator.IS_IN_SEGMENT.getName() : SegmentComparator.IS_NOT_IN_SEGMENT.getName();
        append("Segment evaluation result: User " + segmentResultComparator + ".");
        newLine();
        append("Condition (" + LogHelper.formatSegmentFlagCondition(segmentCondition, segment) + ") evaluates to " + result + ".");
        decreaseIndentLevel();
        newLine();
        append(")");
    }

    public void logSegmentEvaluationError(SegmentCondition segmentCondition, Segment segment, String error) {
        if (!isLoggable) {
            return;
        }
        newLine();

        append("Segment evaluation result: " + error + ".");
        newLine();
        append("Condition (" + LogHelper.formatSegmentFlagCondition(segmentCondition, segment) + ") failed to evaluate.");
        decreaseIndentLevel();
        newLine();
        append(")");
    }

    public void logPrerequisiteFlagEvaluationStart(String prerequisiteFlagKey) {
        if (!isLoggable) {
            return;
        }
        newLine();
        append("(");
        increaseIndentLevel();
        newLine();
        append("Evaluating prerequisite flag '" + prerequisiteFlagKey + "':");
    }

    public void logPrerequisiteFlagEvaluationResult(PrerequisiteFlagCondition prerequisiteFlagCondition, SettingsValue prerequisiteFlagValue, boolean result) {
        if (!isLoggable) {
            return;
        }
        newLine();
        String prerequisiteFlagValueFormat = prerequisiteFlagValue != null ? prerequisiteFlagValue.toString() : LogHelper.INVALID_VALUE;
        append("Prerequisite flag evaluation result: '" + prerequisiteFlagValueFormat + "'.");
        newLine();
        append("Condition (" + LogHelper.formatPrerequisiteFlagCondition(prerequisiteFlagCondition) + ") evaluates to " + result + ".");
        decreaseIndentLevel();
        newLine();
        append(")");
    }
}
