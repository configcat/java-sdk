package com.configcat;

import de.skuzzle.semantic.Version;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class RolloutEvaluator {
    private final ConfigCatLogger logger;

    public RolloutEvaluator(ConfigCatLogger logger) {
        this.logger = logger;
    }

    public EvaluationResult evaluate(Setting setting, String key, User user, Map<String, Setting> settings, EvaluateLogger evaluateLogger) {
        try {
            evaluateLogger.logEvaluation(key);
            if (user != null) {
                evaluateLogger.logUserObject(user);
            }
            evaluateLogger.increaseIndentLevel();


            EvaluationContext context = new EvaluationContext(key, user, null, settings);

            EvaluationResult evaluationResult = evaluateSetting(setting, evaluateLogger, context);

            evaluateLogger.logReturnValue(evaluationResult.value.toString());
            evaluateLogger.decreaseIndentLevel();
            return evaluationResult;
        } finally {
            if (evaluateLogger.isLoggable()) {
                this.logger.info(5000, evaluateLogger.toPrint());
            }
        }
    }

    @NotNull
    private EvaluationResult evaluateSetting(Setting setting, EvaluateLogger evaluateLogger, EvaluationContext context) {
        EvaluationResult evaluationResult = null;
        if (setting.getTargetingRules() != null) {
            evaluationResult = evaluateTargetingRules(setting, context, evaluateLogger);
        }
        if (evaluationResult == null && setting.getPercentageOptions() != null && setting.getPercentageOptions().length > 0) {
            evaluationResult = evaluatePercentageOptions(setting.getPercentageOptions(), setting.getPercentageAttribute(), context, null, evaluateLogger);
        }
        if (evaluationResult == null) {
            evaluationResult = new EvaluationResult(setting.getSettingsValue(), setting.getVariationId(), null, null);
        }
        return evaluationResult;
    }

    private boolean evaluateUserCondition(UserCondition userCondition, EvaluationContext context, String configSalt, String contextSalt, EvaluateLogger evaluateLogger) throws RolloutEvaluatorException {
        evaluateLogger.append(LogHelper.formatUserCondition(userCondition));

        if (context.getUser() == null) {
            if (!context.isUserMissing()) {
                context.setUserMissing(true);
                this.logger.warn(3001, ConfigCatLogMessages.getUserObjectMissing(context.getKey()));
            }
            throw new RolloutEvaluatorException("cannot evaluate, User Object is missing");
        }

        String comparisonAttribute = userCondition.getComparisonAttribute();
        Comparator comparator = Comparator.fromId(userCondition.getComparator());
        String userValue = context.getUser().getAttribute(comparisonAttribute);

        if (userValue == null || userValue.isEmpty()) {
            logger.warn(3003, ConfigCatLogMessages.getUserAttributeMissing(context.getKey(), userCondition, comparisonAttribute));
            throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is missing");
        }


        if (comparator == null) {
            throw new IllegalArgumentException("Comparison operator is invalid.");
        }
        switch (comparator) {
            case CONTAINS_ANY_OF:
                List<String> containsValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
                containsValues.replaceAll(String::trim);
                containsValues.removeAll(Arrays.asList(null, ""));
                for (String containsValue : containsValues) {
                    if (userValue.contains(containsValue))
                        return true;
                }
                return false;
            case NOT_CONTAINS_ANY_OF:
                List<String> notContainsValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
                notContainsValues.replaceAll(String::trim);
                notContainsValues.removeAll(Arrays.asList(null, ""));
                for (String notContainsValue : notContainsValues) {
                    if (userValue.contains(notContainsValue))
                        return false;
                }
                return true;
            case SEMVER_IS_ONE_OF:
            case SEMVER_IS_NOT_ONE_OF:
                List<String> inSemVerValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
                inSemVerValues.replaceAll(String::trim);
                inSemVerValues.removeAll(Arrays.asList(null, ""));
                try {
                    Version userVersion = Version.parseVersion(userValue.trim(), true);
                    boolean matched = false;
                    for (String semVer : inSemVerValues) {
                        matched = userVersion.compareTo(Version.parseVersion(semVer, true)) == 0 || matched;
                    }

                    return (matched && Comparator.SEMVER_IS_ONE_OF.equals(comparator)) || (!matched && Comparator.SEMVER_IS_NOT_ONE_OF.equals(comparator));
                } catch (Exception e) {
                    String reason = "'" + userValue + "' is not a valid semantic version";
                    this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
                    throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is invalid (" + reason + ")");
                }
            case SEMVER_LESS:
            case SEMVER_LESS_EQULAS:
            case SEMVER_GREATER:
            case SEMVER_GREATER_EQUALS:
                try {
                    Version cmpUserVersion = Version.parseVersion(userValue.trim(), true);
                    String comparisonValue = userCondition.getStringValue();
                    Version matchValue = Version.parseVersion(comparisonValue.trim(), true);
                    return (Comparator.SEMVER_LESS.equals(comparator) && cmpUserVersion.isLowerThan(matchValue)) ||
                            (Comparator.SEMVER_LESS_EQULAS.equals(comparator) && cmpUserVersion.compareTo(matchValue) <= 0) ||
                            (Comparator.SEMVER_GREATER.equals(comparator) && cmpUserVersion.isGreaterThan(matchValue)) ||
                            (Comparator.SEMVER_GREATER_EQUALS.equals(comparator) && cmpUserVersion.compareTo(matchValue) >= 0);
                } catch (Exception e) {
                    String reason = "'" + userValue + "' is not a valid semantic version";
                    this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
                    throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is invalid (" + reason + ")");
                }
            case NUMBER_EQUALS:
            case NUMBER_NOT_EQUALS:
            case NUMBER_LESS:
            case NUMBER_LESS_EQUALS:
            case NUMBER_GREATER:
            case NUMBER_GREATER_EQUALS:
                try {
                    Double userDoubleValue = Double.parseDouble(userValue.trim().replaceAll(",", "."));
                    Double comparisonDoubleValue = userCondition.getDoubleValue();

                    return (Comparator.NUMBER_EQUALS.equals(comparator) && userDoubleValue.equals(comparisonDoubleValue)) ||
                            (Comparator.NUMBER_NOT_EQUALS.equals(comparator) && !userDoubleValue.equals(comparisonDoubleValue)) ||
                            (Comparator.NUMBER_LESS.equals(comparator) && userDoubleValue < comparisonDoubleValue) ||
                            (Comparator.NUMBER_LESS_EQUALS.equals(comparator) && userDoubleValue <= comparisonDoubleValue) ||
                            (Comparator.NUMBER_GREATER.equals(comparator) && userDoubleValue > comparisonDoubleValue) ||
                            (Comparator.NUMBER_GREATER_EQUALS.equals(comparator) && userDoubleValue >= comparisonDoubleValue);
                } catch (NumberFormatException e) {
                    String reason = "'" + userValue + "' is not a valid decimal number";
                    this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
                    throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is invalid (" + reason + ")");
                }
            case SENSITIVE_IS_ONE_OF:
                List<String> inValuesSensitive = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
                inValuesSensitive.replaceAll(String::trim);
                inValuesSensitive.removeAll(Arrays.asList(null, ""));
                String hashValueOne = getSaltedUserValue(userValue, configSalt, contextSalt);
                return inValuesSensitive.contains(hashValueOne);
            case SENSITIVE_IS_NOT_ONE_OF:
                List<String> notInValuesSensitive = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
                notInValuesSensitive.replaceAll(String::trim);
                notInValuesSensitive.removeAll(Arrays.asList(null, ""));
                String hashValueNotOne = getSaltedUserValue(userValue, configSalt, contextSalt);
                return !notInValuesSensitive.contains(hashValueNotOne);
            case DATE_BEFORE:
            case DATE_AFTER:
                try {
                    double userDoubleValue = Double.parseDouble(userValue.trim().replaceAll(",", "."));
                    Double comparisonDoubleValue = userCondition.getDoubleValue();
                    return (Comparator.DATE_BEFORE.equals(comparator) && userDoubleValue < comparisonDoubleValue) ||
                            (Comparator.DATE_AFTER.equals(comparator) && userDoubleValue > comparisonDoubleValue);
                } catch (NumberFormatException e) {
                    String reason = "'" + userValue + "' is not a valid Unix timestamp (number of seconds elapsed since Unix epoch)";
                    this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
                    throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is invalid (" + reason + ")");
                }
            case HASHED_EQUALS:
                String hashEquals = getSaltedUserValue(userValue, configSalt, contextSalt);
                return hashEquals.equals(userCondition.getStringValue());
            case HASHED_NOT_EQUALS:
                String hashNotEquals = getSaltedUserValue(userValue, configSalt, contextSalt);
                return !hashNotEquals.equals(userCondition.getStringValue());
            case HASHED_STARTS_WITH:
            case HASHED_ENDS_WITH:
            case HASHED_NOT_STARTS_WITH:
            case HASHED_NOT_ENDS_WITH:
                List<String> withValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
                withValues.replaceAll(String::trim);
                withValues.removeAll(Arrays.asList(null, ""));
                boolean foundEqual = false;
                for (String comparisonValueHashedStartsEnds : withValues) {
                    int indexOf = comparisonValueHashedStartsEnds.indexOf("_");
                    if (indexOf <= 0) {
                        throw new IllegalArgumentException("Comparison value is missing or invalid.");
                    }
                    String comparedTextLength = comparisonValueHashedStartsEnds.substring(0, indexOf);
                    try {
                        int comparedTextLengthInt = Integer.parseInt(comparedTextLength);
                        if (userValue.length() < comparedTextLengthInt) {
                            continue;
                        }
                        String comparisonHashValue = comparisonValueHashedStartsEnds.substring(indexOf + 1);
                        if (comparisonHashValue.isEmpty()) {
                            throw new IllegalArgumentException("Comparison value is missing or invalid.");
                        }
                        String userValueSubString;
                        if (Comparator.HASHED_STARTS_WITH.equals(comparator) || Comparator.HASHED_NOT_STARTS_WITH.equals(comparator)) {
                            userValueSubString = userValue.substring(0, comparedTextLengthInt);
                        } else { //HASHED_ENDS_WITH
                            userValueSubString = userValue.substring(userValue.length() - comparedTextLengthInt);
                        }
                        String hashUserValueSub = getSaltedUserValue(userValueSubString, configSalt, contextSalt);
                        if (hashUserValueSub.equals(comparisonHashValue)) {
                            foundEqual = true;
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Comparison value is missing or invalid.");
                    }
                }
                if (Comparator.HASHED_NOT_STARTS_WITH.equals(comparator) || Comparator.HASHED_NOT_ENDS_WITH.equals(comparator)) {
                    return !foundEqual;
                }
                return foundEqual;
            case HASHED_ARRAY_CONTAINS:
                List<String> containsHashedValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
                String[] userCSVContainsHashSplit = userValue.split(",");
                for (String userValueSlice : userCSVContainsHashSplit) {
                    String userValueSliceHash = getSaltedUserValue(userValueSlice.trim(), configSalt, contextSalt);
                    if (containsHashedValues.contains(userValueSliceHash)) {
                        return true;
                    }
                }
                return false;
            case HASHED_ARRAY_NOT_CONTAINS:
                List<String> notContainsHashedValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
                String[] userCSVNotContainsHashSplit = userValue.split(",");
                if (userCSVNotContainsHashSplit.length == 0) {
                    return false;
                }
                boolean containsFlag = false;
                for (String userValueSlice : userCSVNotContainsHashSplit) {
                    String userValueSliceHash = getSaltedUserValue(userValueSlice.trim(), configSalt, contextSalt);
                    if (notContainsHashedValues.contains(userValueSliceHash)) {
                        containsFlag = true;
                    }
                }
                return !containsFlag;
            default:
                throw new IllegalArgumentException("Comparison operator is invalid.");
        }
    }

    private static String getSaltedUserValue(String userValue, String configJsonSalt, String contextSalt) {
        return new String(Hex.encodeHex(DigestUtils.sha256(userValue + configJsonSalt + contextSalt)));
    }

    private boolean evaluateSegmentCondition(SegmentCondition segmentCondition, EvaluationContext context, String configSalt, Segment[] segments, EvaluateLogger evaluateLogger) {
        int segmentIndex = segmentCondition.getSegmentIndex();
        Segment segment = null;
        if (segmentIndex < segments.length) {
            segment = segments[segmentIndex];
        }
        evaluateLogger.append(LogHelper.formatSegmentFlagCondition(segmentCondition, segment));

        if (context.getUser() == null) {
            if (!context.isUserMissing()) {
                context.setUserMissing(true);
                logger.warn(3001, ConfigCatLogMessages.getUserObjectMissing(context.getKey()));
            }
            throw new RolloutEvaluatorException("cannot evaluate, User Object is missing");
        }

        if (segment == null) {
            throw new IllegalArgumentException("Segment reference is invalid.");
        }
        String segmentName = segment.getName();
        if (segmentName == null || segmentName.isEmpty()) {
            throw new IllegalArgumentException("Segment name is missing.");

        }
        evaluateLogger.logSegmentEvaluationStart(segmentName);
        boolean segmentRulesResult;
        try {
            segmentRulesResult = evaluateConditions(segment.getSegmentRules(), null, context, configSalt, segmentName, segments, evaluateLogger);
        } catch (RolloutEvaluatorException evaluatorException) {
            segmentRulesResult = false;
        }
        boolean result;

        SegmentComparator segmentComparator = SegmentComparator.fromId(segmentCondition.getSegmentComparator());
        if (segmentComparator == null) {
            throw new IllegalArgumentException("Segment comparison operator is invalid.");
        }
        switch (segmentComparator) {
            case IS_IN_SEGMENT:
                result = segmentRulesResult;
                break;
            case IS_NOT_IN_SEGMENT:
                result = !segmentRulesResult;
                break;
            default:
                throw new IllegalArgumentException("Segment comparison operator is invalid.");
        }

        evaluateLogger.logSegmentEvaluationResult(segmentCondition, segment, result, segmentRulesResult);

        return result;
    }

    private boolean evaluatePrerequisiteFlagCondition(PrerequisiteFlagCondition prerequisiteFlagCondition, EvaluationContext context, EvaluateLogger evaluateLogger) {
        evaluateLogger.append(LogHelper.formatPrerequisiteFlagCondition(prerequisiteFlagCondition));

        String prerequisiteFlagKey = prerequisiteFlagCondition.getPrerequisiteFlagKey();
        Setting prerequsiteFlagSetting = context.getSettings().get(prerequisiteFlagKey);
        if (prerequisiteFlagKey == null || prerequisiteFlagKey.isEmpty() || prerequsiteFlagSetting == null) {
            throw new IllegalArgumentException("Prerequisite flag key is missing or invalid.");
        }
        List<String> visitedKeys = context.getVisitedKeys();
        if (visitedKeys == null) {
            visitedKeys = new ArrayList<>();
        }
        visitedKeys.add(context.getKey());
        if (visitedKeys.contains(prerequisiteFlagKey)) {
            String dependencyCycle = LogHelper.formatCircularDependencyList(context.getVisitedKeys(), prerequisiteFlagKey);
            this.logger.warn(3005, ConfigCatLogMessages.getCircularDependencyDetected(context.getKey(), prerequisiteFlagCondition, dependencyCycle));
            throw new RolloutEvaluatorException("cannot evaluate, circular dependency detected");
        }

        evaluateLogger.logPrerequisiteFlagEvaluationStart(prerequisiteFlagKey);

        EvaluationContext prerequisiteFlagContext = new EvaluationContext(prerequisiteFlagKey, context.getUser(), visitedKeys, context.getSettings());

        EvaluationResult evaluateResult = evaluateSetting(prerequsiteFlagSetting, evaluateLogger, prerequisiteFlagContext);
        if (evaluateResult.value == null) {
            return false;
        }

        PrerequisiteComparator prerequisiteComparator = PrerequisiteComparator.fromId(prerequisiteFlagCondition.getPrerequisiteComparator());
        SettingsValue conditionValue = prerequisiteFlagCondition.getValue();
        boolean result;

        if (prerequisiteComparator == null) {
            throw new IllegalArgumentException("Prerequisite Flag comparison operator is invalid.");
        }
        switch (prerequisiteComparator) {
            case EQUALS:
                result = conditionValue.equals(evaluateResult.value);
                break;
            case NOT_EQUALS:
                result = !conditionValue.equals(evaluateResult.value);
                break;
            default:
                throw new IllegalArgumentException("Prerequisite Flag comparison operator is invalid.");
        }

        evaluateLogger.logPrerequisiteFlagEvaluationResult(prerequisiteFlagCondition, evaluateResult.value, result);

        return result;
    }

    private EvaluationResult evaluateTargetingRules(Setting setting, EvaluationContext context, EvaluateLogger evaluateLogger) {

        evaluateLogger.logTargetingRules();
        for (TargetingRule rule : setting.getTargetingRules()) {
            boolean evaluateConditionsResult;
            SettingsValue servedValue = null;
            if (rule.getServedValue() != null) {
                servedValue = rule.getServedValue().getValue();
            }

            evaluateConditionsResult = evaluateConditions(rule.getConditions(), rule, context, setting.getConfigSalt(), context.getKey(), setting.getSegments(), evaluateLogger);

            if (!evaluateConditionsResult) {
                continue;
            }
            if (servedValue != null) {
                return new EvaluationResult(rule.getServedValue().getValue(), rule.getServedValue().getVariationId(), rule, null);
            }

            //if (PO.length <= 0) error case no SV and no PO
            if (rule.getPercentageOptions() == null || rule.getPercentageOptions().length == 0) {
                throw new IllegalArgumentException("Targeting rule THEN part is missing or invalid.");
            }

            evaluateLogger.increaseIndentLevel();
            EvaluationResult evaluatePercentageOptionsResult = evaluatePercentageOptions(rule.getPercentageOptions(), setting.getPercentageAttribute(), context, rule, evaluateLogger);
            evaluateLogger.decreaseIndentLevel();

            if (evaluatePercentageOptionsResult == null) {
                evaluateLogger.logTargetingRuleIgnored();
                continue;
            }

            return evaluatePercentageOptionsResult;

        }
        return null;
    }

    private boolean evaluateConditions(Object[] conditions, TargetingRule targetingRule, EvaluationContext context, String configSalt, String contextSalt, Segment[] segments, EvaluateLogger evaluateLogger) {

        //Conditions are ANDs so if One is not matching return false, if all matching return true
        boolean firstConditionFlag = true;
        boolean conditionsEvaluationResult = false;
        String error = null;
        boolean newLine = false;
        for (Object rawCondition : conditions) {
            if (firstConditionFlag) {
                firstConditionFlag = false;
                evaluateLogger.newLine();
                evaluateLogger.append("- IF ");
                evaluateLogger.increaseIndentLevel();
            } else {
                evaluateLogger.increaseIndentLevel();
                evaluateLogger.newLine();
                evaluateLogger.append("AND ");
            }

            if (targetingRule == null) {
                try {
                    conditionsEvaluationResult = evaluateUserCondition((UserCondition) rawCondition, context, configSalt, contextSalt, evaluateLogger);
                } catch (RolloutEvaluatorException evaluatorException) {
                    error = evaluatorException.getMessage();
                    conditionsEvaluationResult = false;
                }
                newLine = conditions.length > 1;
            } else {
                Condition condition = (Condition) rawCondition;
                if (condition.getComparisonCondition() != null) {
                    try {
                        conditionsEvaluationResult = evaluateUserCondition(condition.getComparisonCondition(), context, configSalt, contextSalt, evaluateLogger);
                    } catch (RolloutEvaluatorException evaluatorException) {
                        error = evaluatorException.getMessage();
                        conditionsEvaluationResult = false;
                    }
                    newLine = conditions.length > 1;
                } else if (condition.getSegmentCondition() != null) {
                    try {
                        conditionsEvaluationResult = evaluateSegmentCondition(condition.getSegmentCondition(), context, configSalt, segments, evaluateLogger);
                    } catch (RolloutEvaluatorException evaluatorException) {
                        error = evaluatorException.getMessage();
                        conditionsEvaluationResult = false;
                    }
                    newLine = error == null || conditions.length > 1;
                } else if (condition.getPrerequisiteFlagCondition() != null) {
                    try {
                        conditionsEvaluationResult = evaluatePrerequisiteFlagCondition(condition.getPrerequisiteFlagCondition(), context, evaluateLogger);
                    } catch (RolloutEvaluatorException evaluatorException) {
                        error = evaluatorException.getMessage();
                        conditionsEvaluationResult = false;
                    }
                    newLine = error == null || conditions.length > 1;
                }
            }


            if (targetingRule == null || conditions.length > 1) {
                evaluateLogger.logConditionConsequence(conditionsEvaluationResult);
            }
            evaluateLogger.decreaseIndentLevel();
            if (!conditionsEvaluationResult) {
                break;
            }
        }
        if (targetingRule != null) {
            evaluateLogger.logTargetingRuleConsequence(targetingRule, error, conditionsEvaluationResult, newLine);
        }
        if (error != null) {
            evaluateLogger.logTargetingRuleIgnored();
        }
        return conditionsEvaluationResult;
    }

    private EvaluationResult evaluatePercentageOptions(PercentageOption[] percentageOptions, String percentageOptionAttribute, EvaluationContext context, TargetingRule parentTargetingRule, EvaluateLogger evaluateLogger) {
        if (context.getUser() == null) {
            evaluateLogger.logPercentageOptionUserMissing();
            if (!context.isUserMissing()) {
                context.setUserMissing(true);
                this.logger.warn(3001, ConfigCatLogMessages.getUserObjectMissing(context.getKey()));
            }
            return null;
        }
        String percentageOptionAttributeValue;
        String percentageOptionAttributeName = percentageOptionAttribute;
        if (percentageOptionAttributeName == null || percentageOptionAttributeName.isEmpty()) {
            percentageOptionAttributeName = "Identifier";
            percentageOptionAttributeValue = context.getUser().getIdentifier();
        } else {
            percentageOptionAttributeValue = context.getUser().getAttribute(percentageOptionAttributeName);
            if (percentageOptionAttributeValue == null) {
                evaluateLogger.logPercentageOptionUserAttributeMissing(percentageOptionAttributeName);
                if (!context.isUserAttributeMissing()) {
                    context.setUserAttributeMissing(true);
                    this.logger.warn(3003, ConfigCatLogMessages.getUserAttributeMissing(context.getKey(), percentageOptionAttributeName));
                }
                return null;
            }
        }

        evaluateLogger.logPercentageOptionEvaluation(percentageOptionAttributeName);
        String hashCandidate = context.getKey() + percentageOptionAttributeValue;
        int scale = 100;
        String hexHash = new String(Hex.encodeHex(DigestUtils.sha1(hashCandidate))).substring(0, 7);
        int longHash = Integer.parseInt(hexHash, 16);
        int scaled = longHash % scale;
        evaluateLogger.logPercentageOptionEvaluationHash(percentageOptionAttributeName, scaled);

        int bucket = 0;

        for (int i = 0; i < percentageOptions.length; i++) {
            PercentageOption rule = percentageOptions[i];
            bucket += rule.getPercentage();
            if (scaled < bucket) {
                evaluateLogger.logPercentageEvaluationReturnValue(scaled, i, rule.getPercentage(), rule.getValue());
                return new EvaluationResult(rule.getValue(), rule.getVariationId(), parentTargetingRule, rule);
            }
        }
        return null;
    }

}

class RolloutEvaluatorException extends RuntimeException {
    public RolloutEvaluatorException(String message) {
        super(message);
    }
}
