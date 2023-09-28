package com.configcat;

public class EvaluateLogger {

    private final StringBuilder stringBuilder = new StringBuilder();

    public EvaluateLogger() {
        indentLevel = 0;
    }

    private int indentLevel;

    public final void logUserObject(final User user) {
        append(" for User '" + user.toString() + "'");
    }

    public final void logEvaluation(String key) {
        append("Evaluating '" + key + "'");
    }

    public final void logPercentageOptionUserMissing() {
        newLine();
        append("Skipping % options because the User Object is missing.");
    }

    public final void logPercentageOptionUserAttributeMissing(String percentageOptionsAttributeName) {
        newLine();
        append("Skipping % options because the User." + percentageOptionsAttributeName + " attribute is missing.");
    }

    public final void logPercentageOptionEvaluation(String percentageOptionsAttributeName) {
        newLine();
        append("Evaluating % options based on the User." + percentageOptionsAttributeName + " attribute:");
    }

    public final void logPercentageOptionEvaluationHash(String percentageOptionsAttributeName, int hashValue) {
        newLine();
        append("- Computing hash in the [0..99] range from User." + percentageOptionsAttributeName + " => " + hashValue + " (this value is sticky and consistent across all SDKs)");
    }

    public final void append(final String line) {
        stringBuilder.append(line);
    }

    public final void increaseIndentLevel() {
        indentLevel++;
    }

    public final void decreaseIndentLevel() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }

    public final void newLine() {
        stringBuilder.append("\n");
        stringBuilder.append("  ".repeat(Math.max(0, indentLevel)));
    }

    public String toPrint() {
        return stringBuilder.toString();
    }

    public void logReturnValue(String returnValue) {
        newLine();
        append("Returning '" + returnValue + "'.");
    }

    public void logTargetingRules() {
        newLine();
        append("Evaluating targeting rules and applying the first match if any:");
    }

    public void logConditionConsequence(boolean result) {
        append(" => " + result);
        if (!result) {
            append(", skipping the remaining AND conditions");
        }
    }

    public void logTargetingRuleIgnored() {
        increaseIndentLevel();
        newLine();
        append("The current targeting rule is ignored and the evaluation continues with the next rule.");
        decreaseIndentLevel();
    }

    public void logTargetingRuleConsequence(TargetingRule targetingRule, String error, boolean isMatch, boolean newLine) {
        increaseIndentLevel();
        String valueFormat = "% options";
        if (targetingRule != null && targetingRule.getServedValue() != null && targetingRule.getServedValue().getValue() != null) {
            valueFormat = "'" + targetingRule.getServedValue().getValue() + "'";
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
        String percentageOptionValue = settingsValue != null ? settingsValue.toString() : LogHelper.INVALID_VALUE;
        newLine();
        append("- Hash value " + hashValue + " selects % option " + (i + 1) + " (" + percentage + "%), '" + percentageOptionValue + "'.");
    }

    public void logSegmentEvaluationStart(String segmentName) {
        newLine();
        append("(");
        increaseIndentLevel();
        newLine();
        append("Evaluating segment '" + segmentName + "':");
    }

    public void logSegmentEvaluationResult(SegmentCondition segmentCondition, Segment segment, boolean result, boolean segmentResult) {
        newLine();
        String segmentResultComparator = segmentResult ? SegmentComparator.IS_IN_SEGMENT.getName() : SegmentComparator.IS_NOT_IN_SEGMENT.getName();
        append("Segment evaluation result: User " + segmentResultComparator + ".");
        newLine();
        append("Condition (" + LogHelper.formatSegmentFlagCondition(segmentCondition, segment) + ") evaluates to " + result + ".");
        decreaseIndentLevel();
        newLine();
        append(")");
    }

    public void logPrerequisiteFlagEvaluationStart(String prerequisiteFlagKey) {
        newLine();
        append("(");
        increaseIndentLevel();
        newLine();
        append("Evaluating prerequisite flag '" + prerequisiteFlagKey + "':");
    }

    public void logPrerequisiteFlagEvaluationResult(PrerequisiteFlagCondition prerequisiteFlagCondition, SettingsValue prerequisiteFlagValue, boolean result) {
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
