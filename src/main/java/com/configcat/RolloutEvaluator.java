package com.configcat;

import com.google.gson.JsonSyntaxException;
import de.skuzzle.semantic.Version;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class RolloutEvaluator {
    public static final String USER_OBJECT_IS_MISSING = "cannot evaluate, User Object is missing";
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
            throw new RolloutEvaluatorException(USER_OBJECT_IS_MISSING);
        }

        String comparisonAttribute = userCondition.getComparisonAttribute();
        Comparator comparator = Comparator.fromId(userCondition.getComparator());
        String userAttributeValue = context.getUser().getAttribute(comparisonAttribute);

        if (userAttributeValue == null || userAttributeValue.isEmpty()) {
            logger.warn(3003, ConfigCatLogMessages.getUserAttributeMissing(context.getKey(), userCondition, comparisonAttribute));
            throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is missing");
        }

        if (comparator == null) {
            throw new IllegalArgumentException("Comparison operator is invalid.");
        }
        switch (comparator) {
            case CONTAINS_ANY_OF:
            case NOT_CONTAINS_ANY_OF:
                boolean negateContainsAnyOf = Comparator.NOT_CONTAINS_ANY_OF.equals(comparator);
                return evaluateContainsAnyOf(userCondition, userAttributeValue, negateContainsAnyOf);
            case SEMVER_IS_ONE_OF:
            case SEMVER_IS_NOT_ONE_OF:
                boolean negateSemverIsOneOf = Comparator.SEMVER_IS_NOT_ONE_OF.equals(comparator);
                return evaluateSemverIsOneOf(userCondition, context, comparisonAttribute, userAttributeValue, negateSemverIsOneOf);
            case SEMVER_LESS:
            case SEMVER_LESS_EQULAS:
            case SEMVER_GREATER:
            case SEMVER_GREATER_EQUALS:
                return evaluateSemver(userCondition, context, comparisonAttribute, comparator, userAttributeValue);
            case NUMBER_EQUALS:
            case NUMBER_NOT_EQUALS:
            case NUMBER_LESS:
            case NUMBER_LESS_EQUALS:
            case NUMBER_GREATER:
            case NUMBER_GREATER_EQUALS:
                return evaluateNumbers(userCondition, context, comparisonAttribute, comparator, userAttributeValue);
            case IS_ONE_OF:
            case IS_NOT_ONE_OF:
            case SENSITIVE_IS_ONE_OF:
            case SENSITIVE_IS_NOT_ONE_OF:
                boolean negateIsOneOf = Comparator.SENSITIVE_IS_NOT_ONE_OF.equals(comparator) || Comparator.IS_NOT_ONE_OF.equals(comparator);
                boolean sensitiveIsOneOf = Comparator.SENSITIVE_IS_ONE_OF.equals(comparator) || Comparator.SENSITIVE_IS_NOT_ONE_OF.equals(comparator);
                return evaluateIsOneOf(userCondition, configSalt, contextSalt, userAttributeValue, negateIsOneOf, sensitiveIsOneOf);
            case DATE_BEFORE:
            case DATE_AFTER:
                return evaluateDate(userCondition, context, comparisonAttribute, comparator, userAttributeValue);
            case TEXT_EQUALS:
            case TEXT_NOT_EQUALS:
            case HASHED_EQUALS:
            case HASHED_NOT_EQUALS:
                boolean negateEquals = Comparator.HASHED_NOT_EQUALS.equals(comparator) || Comparator.TEXT_NOT_EQUALS.equals(comparator);
                boolean hashedEquals = Comparator.HASHED_EQUALS.equals(comparator) || Comparator.HASHED_NOT_EQUALS.equals(comparator);
                return evaluateEquals(userCondition, configSalt, contextSalt, userAttributeValue, negateEquals, hashedEquals);
            case HASHED_STARTS_WITH:
            case HASHED_ENDS_WITH:
            case HASHED_NOT_STARTS_WITH:
            case HASHED_NOT_ENDS_WITH:
                return evaluateHashedStartOrEndsWith(userCondition, configSalt, contextSalt, comparator, userAttributeValue);
            case TEXT_STARTS_WITH:
            case TEXT_NOT_STARTS_WITH:
                boolean negateTextStartWith = Comparator.TEXT_NOT_STARTS_WITH.equals(comparator);
                return evaluateTextStartsWith(userCondition, userAttributeValue, negateTextStartWith);
            case TEXT_ENDS_WITH:
            case TEXT_NOT_ENDS_WITH:
                boolean negateTextEndsWith = Comparator.TEXT_NOT_ENDS_WITH.equals(comparator);
                return evaluateTextEndsWith(userCondition, userAttributeValue, negateTextEndsWith);
            case TEXT_ARRAY_CONTAINS:
            case TEXT_ARRAY_NOT_CONTAINS:
            case HASHED_ARRAY_CONTAINS:
            case HASHED_ARRAY_NOT_CONTAINS:
                boolean negateArrayContains = Comparator.HASHED_ARRAY_NOT_CONTAINS.equals(comparator) || Comparator.TEXT_ARRAY_NOT_CONTAINS.equals(comparator);
                boolean hashedArrayContains = Comparator.HASHED_ARRAY_CONTAINS.equals(comparator) || Comparator.HASHED_ARRAY_NOT_CONTAINS.equals(comparator);
                return evaluateArrayContains(userCondition, context, configSalt, contextSalt, comparisonAttribute, userAttributeValue, negateArrayContains, hashedArrayContains);
            default:
                throw new IllegalArgumentException("Comparison operator is invalid.");
        }
    }

    private boolean evaluateHashedStartOrEndsWith(UserCondition userCondition, String configSalt, String contextSalt, Comparator comparator, String userAttributeValue) {
        List<String> withValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
        byte[] userAttributeValueUTF8 = userAttributeValue.getBytes(StandardCharsets.UTF_8);
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
                if (userAttributeValueUTF8.length < comparedTextLengthInt) {
                    continue;
                }
                String comparisonHashValue = comparisonValueHashedStartsEnds.substring(indexOf + 1);
                if (comparisonHashValue.isEmpty()) {
                    throw new IllegalArgumentException("Comparison value is missing or invalid.");
                }
                String userValueSubString;
                if (Comparator.HASHED_STARTS_WITH.equals(comparator) || Comparator.HASHED_NOT_STARTS_WITH.equals(comparator)) {
                    userValueSubString = new String(Arrays.copyOfRange(userAttributeValueUTF8, 0, comparedTextLengthInt), StandardCharsets.UTF_8);
                } else { //HASHED_ENDS_WITH
                    userValueSubString = new String(Arrays.copyOfRange(userAttributeValueUTF8, userAttributeValueUTF8.length- comparedTextLengthInt, userAttributeValueUTF8.length), StandardCharsets.UTF_8);
                }
                String hashUserValueSub = getSaltedUserValue(userValueSubString, configSalt, contextSalt);
                if (hashUserValueSub.equals(comparisonHashValue)) {
                    foundEqual = true;
                    break;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Comparison value is missing or invalid.");
            }
        }
        if (Comparator.HASHED_NOT_STARTS_WITH.equals(comparator) || Comparator.HASHED_NOT_ENDS_WITH.equals(comparator)) {
            return !foundEqual;
        }
        return foundEqual;
    }

    private boolean evaluateTextStartsWith(UserCondition userCondition, String userAttributeValue, boolean negateTextStartWith) {
        List<String> withTextValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
        withTextValues.replaceAll(String::trim);
        withTextValues.removeAll(Arrays.asList(null, ""));
        boolean textStartWith = false;
        for (String textValue : withTextValues) {
            if (userAttributeValue.startsWith(textValue)) {
                textStartWith = true;
                break;
            }
        }
        if (negateTextStartWith) {
            return !textStartWith;
        }
        return textStartWith;
    }

    private boolean evaluateTextEndsWith(UserCondition userCondition, String userAttributeValue, boolean negateTextEndsWith) {
        List<String> withTextValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
        withTextValues.replaceAll(String::trim);
        withTextValues.removeAll(Arrays.asList(null, ""));
        boolean textEndsWith = false;
        for (String textValue : withTextValues) {
            if (userAttributeValue.endsWith(textValue)) {
                textEndsWith = true;
                break;
            }
        }
        if (negateTextEndsWith) {
            return !textEndsWith;
        }
        return textEndsWith;
    }

    private boolean evaluateArrayContains(UserCondition userCondition, EvaluationContext context, String configSalt, String contextSalt, String comparisonAttribute, String userAttributeValue, boolean negateArrayContains, boolean hashedArrayContains) {
        List<String> conditionContainsValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
        String[] userContainsValues;
        try {
            userContainsValues = Utils.gson.fromJson(userAttributeValue, String[].class);
        } catch (JsonSyntaxException exception) {
            String reason = "'" + userAttributeValue + "' is not a valid JSON string array";
            this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
            throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is invalid (" + reason + ")");
        }
        if (userContainsValues.length == 0) {
            return false;
        }
        boolean containsFlag = false;
        for (String userContainsValue : userContainsValues) {
            String userContainsValueConverted;
            if (hashedArrayContains) {
                userContainsValueConverted = getSaltedUserValue(userContainsValue.trim(), configSalt, contextSalt);
            } else {
                userContainsValueConverted = userContainsValue;
            }
            if (conditionContainsValues.contains(userContainsValueConverted)) {
                containsFlag = true;
                break;
            }
        }
        if (negateArrayContains) {
            containsFlag = !containsFlag;
        }
        return containsFlag;
    }

    private boolean evaluateEquals(UserCondition userCondition, String configSalt, String contextSalt, String userValue, boolean negateEquals, boolean hashedEquals) {
        String valueEquals;
        if (hashedEquals) {
            valueEquals = getSaltedUserValue(userValue, configSalt, contextSalt);
        } else {
            valueEquals = userValue;
        }
        boolean equalsResult = valueEquals.equals(userCondition.getStringValue());
        if (negateEquals) {
            equalsResult = !equalsResult;
        }
        return equalsResult;
    }

    private boolean evaluateDate(UserCondition userCondition, EvaluationContext context, String comparisonAttribute, Comparator comparator, String userValue) {
        try {
            double userDoubleValue = Double.parseDouble(userValue.trim().replace(",", "."));
            Double comparisonDoubleValue = userCondition.getDoubleValue();
            return (Comparator.DATE_BEFORE.equals(comparator) && userDoubleValue < comparisonDoubleValue) ||
                    (Comparator.DATE_AFTER.equals(comparator) && userDoubleValue > comparisonDoubleValue);
        } catch (NumberFormatException e) {
            String reason = "'" + userValue + "' is not a valid Unix timestamp (number of seconds elapsed since Unix epoch)";
            this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
            throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is invalid (" + reason + ")");
        }
    }

    private boolean evaluateIsOneOf(UserCondition userCondition, String configSalt, String contextSalt, String userValue, boolean negateIsOneOf, boolean sensitiveIsOneOf) {
        List<String> inValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
        inValues.replaceAll(String::trim);
        inValues.removeAll(Arrays.asList(null, ""));
        String userIsOneOfValue;
        if (sensitiveIsOneOf) {
            userIsOneOfValue = getSaltedUserValue(userValue, configSalt, contextSalt);
        } else {
            userIsOneOfValue = userValue;
        }
        boolean isOneOf = inValues.contains(userIsOneOfValue);
        if (negateIsOneOf) {
            isOneOf = !isOneOf;
        }
        return isOneOf;
    }

    private boolean evaluateNumbers(UserCondition userCondition, EvaluationContext context, String comparisonAttribute, Comparator comparator, String userValue) {
        try {
            Double userDoubleValue = Double.parseDouble(userValue.trim().replace(",", "."));
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
    }

    private boolean evaluateSemver(UserCondition userCondition, EvaluationContext context, String comparisonAttribute, Comparator comparator, String userValue) {
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
    }

    private boolean evaluateSemverIsOneOf(UserCondition userCondition, EvaluationContext context, String comparisonAttribute, String userValue, boolean negate) {
        List<String> inSemVerValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
        inSemVerValues.replaceAll(String::trim);
        inSemVerValues.removeAll(Arrays.asList(null, ""));
        try {
            Version userVersion = Version.parseVersion(userValue.trim(), true);
            boolean matched = false;
            for (String semVer : inSemVerValues) {
                matched = userVersion.compareTo(Version.parseVersion(semVer, true)) == 0 || matched;
            }

            if (negate) {
                matched = !matched;
            }
            return matched;
        } catch (Exception e) {
            String reason = "'" + userValue + "' is not a valid semantic version";
            this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
            throw new RolloutEvaluatorException("cannot evaluate, the User." + comparisonAttribute + " attribute is invalid (" + reason + ")");
        }
    }

    private boolean evaluateContainsAnyOf(UserCondition userCondition, String userValue, boolean negate) {
        boolean containsResult = !negate;
        List<String> containsValues = new ArrayList<>(Arrays.asList(userCondition.getStringArrayValue()));
        containsValues.replaceAll(String::trim);
        containsValues.removeAll(Arrays.asList(null, ""));
        for (String containsValue : containsValues) {
            if (userValue.contains(containsValue))
                return containsResult;
        }
        return !containsResult;
    }

    private static String getSaltedUserValue(String userValue, String configJsonSalt, String contextSalt) {
        //userValue + configJsonSalt + contextSalt
        byte[] toHashBytes;
        byte[] userValueBytes = userValue.getBytes(StandardCharsets.UTF_8);
        byte[] configJsonSaltBytes = configJsonSalt.getBytes(StandardCharsets.UTF_8);
        byte[] contextSaltBytes = contextSalt.getBytes(StandardCharsets.UTF_8);
        toHashBytes = new byte[userValueBytes.length + configJsonSaltBytes.length + contextSaltBytes.length];
        System.arraycopy(userValueBytes, 0, toHashBytes, 0, userValueBytes.length);
        System.arraycopy(configJsonSaltBytes, 0, toHashBytes, userValueBytes.length, configJsonSaltBytes.length);
        System.arraycopy(contextSaltBytes, 0, toHashBytes, userValueBytes.length + configJsonSaltBytes.length, contextSaltBytes.length);

        return new String(Hex.encodeHex(DigestUtils.sha256(toHashBytes)));
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
            throw new RolloutEvaluatorException(USER_OBJECT_IS_MISSING);
        }

        if (segment == null) {
            throw new IllegalArgumentException("Segment reference is invalid.");
        }
        String segmentName = segment.getName();
        if (segmentName == null || segmentName.isEmpty()) {
            throw new IllegalArgumentException("Segment name is missing.");
        }
        evaluateLogger.logSegmentEvaluationStart(segmentName);
        boolean result;
        try {
            boolean segmentRulesResult = evaluateConditions(segment.getSegmentRules(), null, context, configSalt, segmentName, segments, evaluateLogger);

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

        } catch (RolloutEvaluatorException evaluatorException) {
            evaluateLogger.logSegmentEvaluationError(segmentCondition, segment, evaluatorException.getMessage());
            throw evaluatorException;
        }

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
            String error = null;
            try {
                evaluateConditionsResult = evaluateConditions(rule.getConditions(), rule, context, setting.getConfigSalt(), context.getKey(), setting.getSegments(), evaluateLogger);
            } catch (RolloutEvaluatorException rolloutEvaluatorException) {
                error = rolloutEvaluatorException.getMessage();
                evaluateConditionsResult = false;
            }

            if (!evaluateConditionsResult) {
                if (error != null) {
                    evaluateLogger.logTargetingRuleIgnored();
                }
                continue;
            }
            if (servedValue != null) {
                return new EvaluationResult(rule.getServedValue().getValue(), rule.getServedValue().getVariationId(), rule, null);
            }

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
                    newLine = !USER_OBJECT_IS_MISSING.equals(error) || conditions.length > 1;
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
            throw new RolloutEvaluatorException(error);
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
