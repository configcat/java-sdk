package com.configcat;

import de.skuzzle.semantic.Version;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

class RolloutEvaluator {
    public static final String USER_OBJECT_IS_MISSING = "cannot evaluate, User Object is missing";
    public static final String CANNOT_EVALUATE_THE_USER_PREFIX = "cannot evaluate, the User.";
    public static final String COMPARISON_OPERATOR_IS_INVALID = "Comparison operator is invalid.";
    public static final String COMPARISON_VALUE_IS_MISSING_OR_INVALID = "Comparison value is missing or invalid.";
    public static final String CANNOT_EVALUATE_THE_USER_INVALID = " attribute is invalid (";
    public static final String CANNOT_EVALUATE_THE_USER_MISSING = " attribute is missing";
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
        UserComparator userComparator = UserComparator.fromId(userCondition.getComparator());
        Object userAttributeValue = context.getUser().getAttribute(comparisonAttribute);

        if (userAttributeValue == null || (userAttributeValue instanceof String && ((String) userAttributeValue).isEmpty())) {
            logger.warn(3003, ConfigCatLogMessages.getUserAttributeMissing(context.getKey(), userCondition, comparisonAttribute));
            throw new RolloutEvaluatorException(CANNOT_EVALUATE_THE_USER_PREFIX + comparisonAttribute + CANNOT_EVALUATE_THE_USER_MISSING);
        }

        if (userComparator == null) {
            throw new IllegalArgumentException(COMPARISON_OPERATOR_IS_INVALID);
        }
        switch (userComparator) {
            case CONTAINS_ANY_OF:
            case NOT_CONTAINS_ANY_OF:
                boolean negateContainsAnyOf = UserComparator.NOT_CONTAINS_ANY_OF.equals(userComparator);
                String userAttributeForContains = getUserAttributeAsString(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateContainsAnyOf(userCondition, userAttributeForContains, negateContainsAnyOf);
            case SEMVER_IS_ONE_OF:
            case SEMVER_IS_NOT_ONE_OF:
                boolean negateSemverIsOneOf = UserComparator.SEMVER_IS_NOT_ONE_OF.equals(userComparator);
                Version userAttributeValueForSemverIsOneOf = getUserAttributeAsVersion(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateSemverIsOneOf(userCondition, userAttributeValueForSemverIsOneOf, negateSemverIsOneOf);
            case SEMVER_LESS:
            case SEMVER_LESS_EQUALS:
            case SEMVER_GREATER:
            case SEMVER_GREATER_EQUALS:
                Version userAttributeValueForSemverOperators = getUserAttributeAsVersion(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateSemver(userCondition, userComparator, userAttributeValueForSemverOperators);
            case NUMBER_EQUALS:
            case NUMBER_NOT_EQUALS:
            case NUMBER_LESS:
            case NUMBER_LESS_EQUALS:
            case NUMBER_GREATER:
            case NUMBER_GREATER_EQUALS:
                Double userAttributeAsDouble = getUserAttributeAsDouble(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateNumbers(userCondition, userComparator, userAttributeAsDouble);
            case IS_ONE_OF:
            case IS_NOT_ONE_OF:
            case SENSITIVE_IS_ONE_OF:
            case SENSITIVE_IS_NOT_ONE_OF:
                boolean negateIsOneOf = UserComparator.SENSITIVE_IS_NOT_ONE_OF.equals(userComparator) || UserComparator.IS_NOT_ONE_OF.equals(userComparator);
                boolean sensitiveIsOneOf = UserComparator.SENSITIVE_IS_ONE_OF.equals(userComparator) || UserComparator.SENSITIVE_IS_NOT_ONE_OF.equals(userComparator);
                String userAttributeForIsOneOf = getUserAttributeAsString(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateIsOneOf(userCondition, configSalt, contextSalt, userAttributeForIsOneOf, negateIsOneOf, sensitiveIsOneOf);
            case DATE_BEFORE:
            case DATE_AFTER:
                double userAttributeForDate = getUserAttributeForDate(userCondition, context, comparisonAttribute, userAttributeValue);
                return evaluateDate(userCondition, userComparator, userAttributeForDate);
            case TEXT_EQUALS:
            case TEXT_NOT_EQUALS:
            case HASHED_EQUALS:
            case HASHED_NOT_EQUALS:
                boolean negateEquals = UserComparator.HASHED_NOT_EQUALS.equals(userComparator) || UserComparator.TEXT_NOT_EQUALS.equals(userComparator);
                boolean hashedEquals = UserComparator.HASHED_EQUALS.equals(userComparator) || UserComparator.HASHED_NOT_EQUALS.equals(userComparator);
                String userAttributeForEquals = getUserAttributeAsString(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateEquals(userCondition, configSalt, contextSalt, userAttributeForEquals, negateEquals, hashedEquals);
            case HASHED_STARTS_WITH:
            case HASHED_ENDS_WITH:
            case HASHED_NOT_STARTS_WITH:
            case HASHED_NOT_ENDS_WITH:
                String userAttributeForHashedStartEnd = getUserAttributeAsString(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateHashedStartOrEndsWith(userCondition, configSalt, contextSalt, userComparator, userAttributeForHashedStartEnd);
            case TEXT_STARTS_WITH:
            case TEXT_NOT_STARTS_WITH:
                boolean negateTextStartWith = UserComparator.TEXT_NOT_STARTS_WITH.equals(userComparator);
                String userAttributeForTextStart = getUserAttributeAsString(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateTextStartsWith(userCondition, userAttributeForTextStart, negateTextStartWith);
            case TEXT_ENDS_WITH:
            case TEXT_NOT_ENDS_WITH:
                boolean negateTextEndsWith = UserComparator.TEXT_NOT_ENDS_WITH.equals(userComparator);
                String userAttributeForTextEnd = getUserAttributeAsString(context.getKey(), userCondition, comparisonAttribute, userAttributeValue);
                return evaluateTextEndsWith(userCondition, userAttributeForTextEnd, negateTextEndsWith);
            case TEXT_ARRAY_CONTAINS:
            case TEXT_ARRAY_NOT_CONTAINS:
            case HASHED_ARRAY_CONTAINS:
            case HASHED_ARRAY_NOT_CONTAINS:
                boolean negateArrayContains = UserComparator.HASHED_ARRAY_NOT_CONTAINS.equals(userComparator) || UserComparator.TEXT_ARRAY_NOT_CONTAINS.equals(userComparator);
                boolean hashedArrayContains = UserComparator.HASHED_ARRAY_CONTAINS.equals(userComparator) || UserComparator.HASHED_ARRAY_NOT_CONTAINS.equals(userComparator);
                String[] userAttributeAsStringArray = getUserAttributeAsStringArray(userCondition, context, comparisonAttribute, userAttributeValue);
                return evaluateArrayContains(userCondition, configSalt, contextSalt, userAttributeAsStringArray, negateArrayContains, hashedArrayContains);
            default:
                throw new IllegalArgumentException(COMPARISON_OPERATOR_IS_INVALID);
        }
    }

    @SuppressWarnings("unchecked")
    private String[] getUserAttributeAsStringArray(UserCondition userCondition, EvaluationContext context, String comparisonAttribute, Object userAttributeValue) {
        String[] result = null;
        try {
            if (userAttributeValue instanceof String[]) {
                result = (String[]) userAttributeValue;
            } else if (userAttributeValue instanceof List) {
                List<String> list = (List<String>) userAttributeValue;
                String[] userValueArray = new String[list.size()];
                list.toArray(userValueArray);
                result = userValueArray;
            } else if (userAttributeValue instanceof String) {
                result = Utils.gson.fromJson((String) userAttributeValue, String[].class);
            }
            if (result != null && Arrays.stream(result).noneMatch(Objects::isNull)) {
                return result;
            }
        } catch (Exception exception) {
            // String array parse failed continue with the RolloutEvaluatorException
        }
        String reason = "'" + userAttributeValue + "' is not a valid JSON string array";
        this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
        throw new RolloutEvaluatorException(CANNOT_EVALUATE_THE_USER_PREFIX + comparisonAttribute + CANNOT_EVALUATE_THE_USER_INVALID + reason + ")");
    }

    private double getUserAttributeForDate(UserCondition userCondition, EvaluationContext context, String comparisonAttribute, Object userAttributeValue) {
        try {
            if (userAttributeValue instanceof Date) {
                return DateTimeUtils.getUnixSeconds((Date) userAttributeValue);
            }
            if (userAttributeValue instanceof Instant) {
                return DateTimeUtils.getUnixSeconds((Instant) userAttributeValue);
            }
            return UserAttributeConverter.userAttributeToDouble(userAttributeValue);
        } catch (Exception e) {
            String reason = "'" + userAttributeValue + "' is not a valid Unix timestamp (number of seconds elapsed since Unix epoch)";
            this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(context.getKey(), userCondition, reason, comparisonAttribute));
            throw new RolloutEvaluatorException(CANNOT_EVALUATE_THE_USER_PREFIX + comparisonAttribute + CANNOT_EVALUATE_THE_USER_INVALID + reason + ")");
        }
    }

    private String getUserAttributeAsString(String key, UserCondition userCondition, String userAttributeName, Object userAttributeValue) {
        if (userAttributeValue instanceof String) {
            return (String) userAttributeValue;
        }

        String convertedUserAttribute = UserAttributeConverter.userAttributeToString(userAttributeValue);
        this.logger.warn(3005, ConfigCatLogMessages.getUserObjectAttributeIsAutoConverted(key, userCondition, userAttributeName, convertedUserAttribute));
        return convertedUserAttribute;
    }

    private Version getUserAttributeAsVersion(String key, UserCondition userCondition, String comparisonAttribute, Object userValue) {
        if (userValue instanceof String) {
            try {
                return Version.parseVersion(((String) userValue).trim(), true);
            } catch (Version.VersionFormatException e) {
                // Version parse failed continue with the RolloutEvaluatorException
            }
        }
        String reason = "'" + userValue + "' is not a valid semantic version";
        this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(key, userCondition, reason, comparisonAttribute));
        throw new RolloutEvaluatorException(CANNOT_EVALUATE_THE_USER_PREFIX + comparisonAttribute + CANNOT_EVALUATE_THE_USER_INVALID + reason + ")");
    }

    private Double getUserAttributeAsDouble(String key, UserCondition userCondition, String comparisonAttribute, Object userAttributeValue) {
        try {
            if (userAttributeValue instanceof Double) {
                return (Double) userAttributeValue;
            }
            return UserAttributeConverter.userAttributeToDouble(userAttributeValue);
        } catch (NumberFormatException e) {
            //If cannot convert to double, continue with the error
            String reason = "'" + userAttributeValue + "' is not a valid decimal number";
            this.logger.warn(3004, ConfigCatLogMessages.getUserAttributeInvalid(key, userCondition, reason, comparisonAttribute));
            throw new RolloutEvaluatorException(CANNOT_EVALUATE_THE_USER_PREFIX + comparisonAttribute + CANNOT_EVALUATE_THE_USER_INVALID + reason + ")");
        }
    }

    private boolean evaluateHashedStartOrEndsWith(UserCondition userCondition, String configSalt, String contextSalt, UserComparator userComparator, String userAttributeValue) {
        String[] comparisonValues = ensureComparisonValue(userCondition.getStringArrayValue());

        byte[] userAttributeValueUTF8 = userAttributeValue.getBytes(StandardCharsets.UTF_8);
        boolean foundEqual = false;
        for (String comparisonValueHashedStartsEnds : comparisonValues) {
            int indexOf = ensureComparisonValue(comparisonValueHashedStartsEnds).indexOf("_");
            if (indexOf <= 0) {
                throw new IllegalArgumentException(COMPARISON_VALUE_IS_MISSING_OR_INVALID);
            }
            String comparedTextLength = comparisonValueHashedStartsEnds.substring(0, indexOf);
            int comparedTextLengthInt;
            try {
                comparedTextLengthInt = Integer.parseInt(comparedTextLength);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(COMPARISON_VALUE_IS_MISSING_OR_INVALID);
            }

            if (userAttributeValueUTF8.length < comparedTextLengthInt) {
                continue;
            }
            String comparisonHashValue = comparisonValueHashedStartsEnds.substring(indexOf + 1);
            if (comparisonHashValue.isEmpty()) {
                throw new IllegalArgumentException(COMPARISON_VALUE_IS_MISSING_OR_INVALID);
            }
            byte[] userValueSubStringByteArray;
            if (UserComparator.HASHED_STARTS_WITH.equals(userComparator) || UserComparator.HASHED_NOT_STARTS_WITH.equals(userComparator)) {
                userValueSubStringByteArray = Arrays.copyOfRange(userAttributeValueUTF8, 0, comparedTextLengthInt);
            } else { //HASHED_ENDS_WITH
                userValueSubStringByteArray = Arrays.copyOfRange(userAttributeValueUTF8, userAttributeValueUTF8.length - comparedTextLengthInt, userAttributeValueUTF8.length);
            }
            String hashUserValueSub = getSaltedUserValueSlice(userValueSubStringByteArray, configSalt, contextSalt);

            if (hashUserValueSub.equals(comparisonHashValue)) {
                foundEqual = true;
                break;
            }
        }
        if (UserComparator.HASHED_NOT_STARTS_WITH.equals(userComparator) || UserComparator.HASHED_NOT_ENDS_WITH.equals(userComparator)) {
            return !foundEqual;
        }
        return foundEqual;
    }

    private boolean evaluateTextStartsWith(UserCondition userCondition, String userAttributeValue, boolean negateTextStartWith) {
        String[] comparisonValues = ensureComparisonValue(userCondition.getStringArrayValue());

        for (String textValue : comparisonValues) {
            if (userAttributeValue.startsWith(ensureComparisonValue(textValue))) {
                return !negateTextStartWith;
            }
        }
        return negateTextStartWith;
    }

    private boolean evaluateTextEndsWith(UserCondition userCondition, String userAttributeValue, boolean negateTextEndsWith) {
        String[] comparisonValues = ensureComparisonValue(userCondition.getStringArrayValue());

        for (String textValue : comparisonValues) {
            if (userAttributeValue.endsWith(ensureComparisonValue(textValue))) {
                return !negateTextEndsWith;
            }
        }
        return negateTextEndsWith;
    }

    private boolean evaluateArrayContains(UserCondition userCondition, String configSalt, String contextSalt, String[] userContainsValues, boolean negateArrayContains, boolean hashedArrayContains) {
        String[] comparisonValues = ensureComparisonValue(userCondition.getStringArrayValue());

        if (userContainsValues.length == 0) {
            return false;
        }
        for (String userContainsValue : userContainsValues) {
            String userContainsValueConverted = hashedArrayContains ? getSaltedUserValue(userContainsValue, configSalt, contextSalt) : userContainsValue;
            for (String inValuesElement : comparisonValues) {
                if (ensureComparisonValue(inValuesElement).equals(userContainsValueConverted)) {
                    return !negateArrayContains;
                }
            }
        }
        return negateArrayContains;
    }

    private boolean evaluateEquals(UserCondition userCondition, String configSalt, String contextSalt, String userValue, boolean negateEquals, boolean hashedEquals) {
        String comparisonValue = ensureComparisonValue(userCondition.getStringValue());

        String valueEquals = hashedEquals ? getSaltedUserValue(userValue, configSalt, contextSalt) : userValue;
        return negateEquals != valueEquals.equals(comparisonValue);
    }

    private boolean evaluateDate(UserCondition userCondition, UserComparator userComparator, double userDoubleValue) {
        double comparisonDoubleValue = ensureComparisonValue(userCondition.getDoubleValue());
        return (UserComparator.DATE_BEFORE.equals(userComparator) && userDoubleValue < comparisonDoubleValue) ||
                (UserComparator.DATE_AFTER.equals(userComparator) && userDoubleValue > comparisonDoubleValue);
    }

    private boolean evaluateIsOneOf(UserCondition userCondition, String configSalt, String contextSalt, String userValue, boolean negateIsOneOf, boolean sensitiveIsOneOf) {
        String[] comparisonValues = ensureComparisonValue(userCondition.getStringArrayValue());

        String userIsOneOfValue = sensitiveIsOneOf ? getSaltedUserValue(userValue, configSalt, contextSalt) : userValue;

        for (String inValuesElement : comparisonValues) {
            if (ensureComparisonValue(inValuesElement).equals(userIsOneOfValue)) {
                return !negateIsOneOf;
            }
        }
        return negateIsOneOf;
    }

    private boolean evaluateNumbers(UserCondition userCondition, UserComparator userComparator, Double userValue) {
        Double comparisonDoubleValue = ensureComparisonValue(userCondition.getDoubleValue());
        return (UserComparator.NUMBER_EQUALS.equals(userComparator) && userValue.equals(comparisonDoubleValue)) ||
                (UserComparator.NUMBER_NOT_EQUALS.equals(userComparator) && !userValue.equals(comparisonDoubleValue)) ||
                (UserComparator.NUMBER_LESS.equals(userComparator) && userValue < comparisonDoubleValue) ||
                (UserComparator.NUMBER_LESS_EQUALS.equals(userComparator) && userValue <= comparisonDoubleValue) ||
                (UserComparator.NUMBER_GREATER.equals(userComparator) && userValue > comparisonDoubleValue) ||
                (UserComparator.NUMBER_GREATER_EQUALS.equals(userComparator) && userValue >= comparisonDoubleValue);
    }

    private boolean evaluateSemver(UserCondition userCondition, UserComparator userComparator, Version userValue) {
        String comparisonValue = ensureComparisonValue(userCondition.getStringValue());
        Version matchValue;
        try {
            matchValue = Version.parseVersion(comparisonValue.trim(), true);
        } catch (Version.VersionFormatException exception) {
            return false;
        }
        return (UserComparator.SEMVER_LESS.equals(userComparator) && userValue.isLowerThan(matchValue)) ||
                (UserComparator.SEMVER_LESS_EQUALS.equals(userComparator) && userValue.compareTo(matchValue) <= 0) ||
                (UserComparator.SEMVER_GREATER.equals(userComparator) && userValue.isGreaterThan(matchValue)) ||
                (UserComparator.SEMVER_GREATER_EQUALS.equals(userComparator) && userValue.compareTo(matchValue) >= 0);
    }

    private boolean evaluateSemverIsOneOf(UserCondition userCondition, Version userVersion, boolean negate) {
        String[] comparisonValues = ensureComparisonValue(userCondition.getStringArrayValue());

        boolean matched = false;
        for (String semVer : comparisonValues) {
            // Previous versions of the evaluation algorithm ignore empty comparison values.
            // We keep this behavior for backward compatibility.
            if (ensureComparisonValue(semVer).isEmpty()) {
                continue;
            }
            try {
                matched = userVersion.compareTo(Version.parseVersion(semVer.trim(), true)) == 0 || matched;
            } catch (Version.VersionFormatException exception) {
                // Previous versions of the evaluation algorithm ignored invalid comparison values.
                // We keep this behavior for backward compatibility.
                return false;
            }
        }

        return negate != matched;
    }

    private boolean evaluateContainsAnyOf(UserCondition userCondition, String userValue, boolean negate) {
        String[] comparisonValues = ensureComparisonValue(userCondition.getStringArrayValue());

        for (String containsValue : comparisonValues) {
            if (userValue.contains(ensureComparisonValue(containsValue))) {
                return !negate;
            }
        }
        return negate;
    }

    private static String getSaltedUserValue(String userValue, String configJsonSalt, String contextSalt) {
        return DigestUtils.sha256Hex(userValue + configJsonSalt + contextSalt);
    }

    private static String getSaltedUserValueSlice(byte[] userValueSliceUTF8, String configJsonSalt, String contextSalt) {
        byte[] configSaltByteArray = configJsonSalt.getBytes(StandardCharsets.UTF_8);
        byte[] contextSaltByteArray = contextSalt.getBytes(StandardCharsets.UTF_8);
        byte[] concatByteArrays = new byte[userValueSliceUTF8.length + configSaltByteArray.length + contextSaltByteArray.length];

        System.arraycopy(userValueSliceUTF8, 0, concatByteArrays, 0, userValueSliceUTF8.length);
        System.arraycopy(configSaltByteArray, 0, concatByteArrays, userValueSliceUTF8.length, configSaltByteArray.length);
        System.arraycopy(contextSaltByteArray, 0, concatByteArrays, userValueSliceUTF8.length + configSaltByteArray.length, contextSaltByteArray.length);
        return DigestUtils.sha256Hex(concatByteArrays);
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

        SettingType settingType = prerequsiteFlagSetting.getType();
        if ((settingType == SettingType.BOOLEAN && prerequisiteFlagCondition.getValue().getBooleanValue() == null) ||
                (settingType == SettingType.STRING && prerequisiteFlagCondition.getValue().getStringValue() == null) ||
                (settingType == SettingType.INT && prerequisiteFlagCondition.getValue().getIntegerValue() == null) ||
                (settingType == SettingType.DOUBLE && prerequisiteFlagCondition.getValue().getDoubleValue() == null)) {
            throw new IllegalArgumentException("Type mismatch between comparison value '" + prerequisiteFlagCondition.getValue() + "' and prerequisite flag '" + prerequisiteFlagKey + "'.");
        }

        List<String> visitedKeys = context.getVisitedKeys();
        if (visitedKeys == null) {
            visitedKeys = new ArrayList<>();
        }
        visitedKeys.add(context.getKey());
        if (visitedKeys.contains(prerequisiteFlagKey)) {
            String dependencyCycle = LogHelper.formatCircularDependencyList(visitedKeys, prerequisiteFlagKey);
            throw new IllegalArgumentException("Circular dependency detected between the following depending flags: " + dependencyCycle + ".");
        }

        evaluateLogger.logPrerequisiteFlagEvaluationStart(prerequisiteFlagKey);

        EvaluationContext prerequisiteFlagContext = new EvaluationContext(prerequisiteFlagKey, context.getUser(), visitedKeys, context.getSettings());

        EvaluationResult evaluateResult = evaluateSetting(prerequsiteFlagSetting, evaluateLogger, prerequisiteFlagContext);

        visitedKeys.remove(context.getKey());

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
            if (rule.getSimpleValue() != null) {
                return new EvaluationResult(rule.getSimpleValue().getValue(), rule.getSimpleValue().getVariationId(), rule, null);
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

    private boolean evaluateConditions(ConditionAccessor[] conditions, TargetingRule targetingRule, EvaluationContext context, String configSalt, String contextSalt, Segment[] segments, EvaluateLogger evaluateLogger) {
        boolean firstConditionFlag = true;
        boolean conditionsEvaluationResult = true;
        String error = null;
        boolean newLine = false;
        for (ConditionAccessor condition : conditions) {
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

            if (condition.getUserCondition() != null) {
                try {
                    conditionsEvaluationResult = evaluateUserCondition(condition.getUserCondition(), context, configSalt, contextSalt, evaluateLogger);
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
                newLine = true;
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
        if (percentageOptionAttributeName == null) {
            percentageOptionAttributeName = "Identifier";
            percentageOptionAttributeValue = context.getUser().getIdentifier();
        } else {
            percentageOptionAttributeValue = UserAttributeConverter.userAttributeToString(context.getUser().getAttribute(percentageOptionAttributeName));
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
        String hexHash = DigestUtils.sha1Hex(hashCandidate.getBytes(StandardCharsets.UTF_8)).substring(0, 7);
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
        throw new IllegalArgumentException("Sum of percentage option percentages are less than 100.");
    }

    private static <T> T ensureComparisonValue(T value) {
        if (value == null) {
            throw new IllegalArgumentException(COMPARISON_VALUE_IS_MISSING_OR_INVALID);
        }
        return value;
    }
}

class RolloutEvaluatorException extends RuntimeException {
    public RolloutEvaluatorException(String message) {
        super(message);
    }
}
