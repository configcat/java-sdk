package com.configcat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EvaluateLogger {

    private static final String HASHED_VALUE = "<hashed value>";
    public static final String INVALID_VALUE = "<invalid value>";
    public static final String INVALID_NAME = "<invalid name>";
    public static final String INVALID_REFERENCE = "<invalid reference>";

    private static final int MAX_LIST_ELEMENT = 10;
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
        stringBuilder.append(System.lineSeparator());
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
        String percentageOptionValue = settingsValue != null ? settingsValue.toString() : INVALID_VALUE;
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
        append("Condition (" + formatSegmentFlagCondition(segmentCondition, segment) + ") evaluates to " + result + ".");
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
        append("Condition (" + formatSegmentFlagCondition(segmentCondition, segment) + ") failed to evaluate.");
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
        String prerequisiteFlagValueFormat = prerequisiteFlagValue != null ? prerequisiteFlagValue.toString() : INVALID_VALUE;
        append("Prerequisite flag evaluation result: '" + prerequisiteFlagValueFormat + "'.");
        newLine();
        append("Condition (" + formatPrerequisiteFlagCondition(prerequisiteFlagCondition) + ") evaluates to " + result + ".");
        decreaseIndentLevel();
        newLine();
        append(")");
    }

    private static String formatStringListComparisonValue(String[] comparisonValue, boolean isSensitive) {
        if (comparisonValue == null) {
            return INVALID_VALUE;
        }
        List<String> comparisonValues = new ArrayList<>(Arrays.asList(comparisonValue));
        if (comparisonValues.isEmpty()) {
            return INVALID_VALUE;
        }
        String formattedList;
        if (isSensitive) {
            String sensitivePostFix = comparisonValues.size() == 1 ? "value" : "values";
            formattedList = "<" + comparisonValues.size() + " hashed " + sensitivePostFix + ">";
        } else {
            String listPostFix = "";
            if (comparisonValues.size() > MAX_LIST_ELEMENT) {
                int count = comparisonValues.size() - MAX_LIST_ELEMENT;
                String countPostFix = count == 1 ? "value" : "values";
                listPostFix = ", ... <" + count + " more " + countPostFix + ">";
            }
            List<String> subList = comparisonValues.subList(0, Math.min(MAX_LIST_ELEMENT, comparisonValues.size()));
            StringBuilder formatListBuilder = new StringBuilder();
            int subListSize = subList.size();
            for (int i = 0; i < subListSize; i++) {
                formatListBuilder.append("'").append(subList.get(i)).append("'");
                if (i != subListSize - 1) {
                    formatListBuilder.append(", ");
                }
            }
            formatListBuilder.append(listPostFix);
            formattedList = formatListBuilder.toString();
        }

        return "[" + formattedList + "]";
    }

    private static String formatStringComparisonValue(String comparisonValue, boolean isSensitive) {
        return "'" + (isSensitive ? HASHED_VALUE : comparisonValue) + "'";
    }

    private static String formatDoubleComparisonValue(Double comparisonValue, boolean isDate) {
        if (comparisonValue == null) {
            return INVALID_VALUE;
        }
        DecimalFormat decimalFormat = Utils.getDecimalFormat();
        if (isDate) {
            return "'" + decimalFormat.format(comparisonValue) + "' (" + DateTimeUtils.doubleToFormattedUTC(comparisonValue) + " UTC)";
        }
        return "'" + decimalFormat.format(comparisonValue) + "'";
    }

    public static String formatUserCondition(UserCondition userCondition) {
        UserComparator userComparator = UserComparator.fromId(userCondition.getComparator());
        if (userComparator == null) {
            throw new IllegalArgumentException("Comparison operator is invalid.");
        }
        String comparisonValue;
        switch (userComparator) {
            case IS_ONE_OF:
            case IS_NOT_ONE_OF:
            case CONTAINS_ANY_OF:
            case NOT_CONTAINS_ANY_OF:
            case SEMVER_IS_ONE_OF:
            case SEMVER_IS_NOT_ONE_OF:
            case TEXT_STARTS_WITH:
            case TEXT_NOT_STARTS_WITH:
            case TEXT_ENDS_WITH:
            case TEXT_NOT_ENDS_WITH:
            case TEXT_ARRAY_CONTAINS:
            case TEXT_ARRAY_NOT_CONTAINS:
                comparisonValue = formatStringListComparisonValue(userCondition.getStringArrayValue(), false);
                break;
            case SEMVER_LESS:
            case SEMVER_LESS_EQUALS:
            case SEMVER_GREATER:
            case SEMVER_GREATER_EQUALS:
            case TEXT_EQUALS:
            case TEXT_NOT_EQUALS:
                comparisonValue = formatStringComparisonValue(userCondition.getStringValue(), false);
                break;
            case NUMBER_EQUALS:
            case NUMBER_NOT_EQUALS:
            case NUMBER_LESS:
            case NUMBER_LESS_EQUALS:
            case NUMBER_GREATER:
            case NUMBER_GREATER_EQUALS:
                comparisonValue = formatDoubleComparisonValue(userCondition.getDoubleValue(), false);
                break;
            case SENSITIVE_IS_ONE_OF:
            case SENSITIVE_IS_NOT_ONE_OF:
            case HASHED_STARTS_WITH:
            case HASHED_NOT_STARTS_WITH:
            case HASHED_ENDS_WITH:
            case HASHED_NOT_ENDS_WITH:
            case HASHED_ARRAY_CONTAINS:
            case HASHED_ARRAY_NOT_CONTAINS:
                comparisonValue = formatStringListComparisonValue(userCondition.getStringArrayValue(), true);
                break;
            case DATE_BEFORE:
            case DATE_AFTER:
                comparisonValue = formatDoubleComparisonValue(userCondition.getDoubleValue(), true);
                break;
            case HASHED_EQUALS:
            case HASHED_NOT_EQUALS:
                comparisonValue = formatStringComparisonValue(userCondition.getStringValue(), true);
                break;
            default:
                comparisonValue = INVALID_VALUE;
        }

        return "User." + userCondition.getComparisonAttribute() + " " + userComparator.getName() + " " + comparisonValue;
    }

    public static String formatSegmentFlagCondition(SegmentCondition segmentCondition, Segment segment) {
        String segmentName;
        if (segment != null) {
            segmentName = segment.getName();
            if (segmentName == null || segmentName.isEmpty()) {
                segmentName = INVALID_NAME;
            }
        } else {
            segmentName = INVALID_REFERENCE;
        }
        SegmentComparator segmentComparator = SegmentComparator.fromId(segmentCondition.getSegmentComparator());
        if (segmentComparator == null) {
            throw new IllegalArgumentException("Segment comparison operator is invalid.");
        }
        return "User " + segmentComparator.getName() + " '" + segmentName + "'";
    }

    public static String formatPrerequisiteFlagCondition(PrerequisiteFlagCondition prerequisiteFlagCondition) {
        String prerequisiteFlagKey = prerequisiteFlagCondition.getPrerequisiteFlagKey();
        PrerequisiteComparator prerequisiteComparator = PrerequisiteComparator.fromId(prerequisiteFlagCondition.getPrerequisiteComparator());
        if (prerequisiteComparator == null) {
            throw new IllegalArgumentException("Prerequisite Flag comparison operator is invalid.");
        }
        SettingsValue prerequisiteValue = prerequisiteFlagCondition.getValue();
        String comparisonValue = prerequisiteValue == null ? INVALID_VALUE : prerequisiteValue.toString();
        return "Flag '" + prerequisiteFlagKey + "' " + prerequisiteComparator.getName() + " '" + comparisonValue + "'";
    }


    public static String formatCircularDependencyList(List<String> visitedKeys, String key) {
        StringBuilder builder = new StringBuilder();
        visitedKeys.forEach(visitedKey -> builder.append("'").append(visitedKey).append("' -> "));
        builder.append("'").append(key).append("'");
        return builder.toString();
    }

}
