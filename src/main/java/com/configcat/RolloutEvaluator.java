package com.configcat;

import de.skuzzle.semantic.Version;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class RolloutEvaluator {
    private final ConfigCatLogger logger;

    public RolloutEvaluator(ConfigCatLogger logger) {
        this.logger = logger;
    }

    public EvaluationResult evaluate(Setting setting, String key, User user, List<String> visitedKeys, Map<String, Setting> settings, EvaluateLogger evaluateLogger) {
        //TODO logger trick implement? in case info? check it
        try {
            if (user != null) {
                evaluateLogger.logUserObject(user);
            }

            if(visitedKeys == null) {
                visitedKeys = new ArrayList<>();
            }
            visitedKeys.add(key);
            EvaluationContext context = new EvaluationContext(key, user, visitedKeys, settings);

            if (setting.getTargetingRules() != null) {
                EvaluationResult targetingRulesEvaluationResult = evaluateTargetingRules(setting, context, evaluateLogger);
                if (targetingRulesEvaluationResult != null) return targetingRulesEvaluationResult;
            }

            if (setting.getPercentageOptions() != null && setting.getPercentageOptions().length > 0) {
                EvaluationResult percentageOptionsEvaluationResult = evaluatePercentageOptions(setting.getPercentageOptions(), setting.getPercentageAttribute(), context, null, evaluateLogger);
                if (percentageOptionsEvaluationResult != null) return percentageOptionsEvaluationResult;
            }

            evaluateLogger.logReturnValue(setting.getSettingsValue().toString());
            return new EvaluationResult(setting.getSettingsValue(), setting.getVariationId(), null, null);
        } finally {
            this.logger.info(5000, evaluateLogger.toPrint());
        }
    }

    private boolean evaluateComparisonCondition(ComparisonCondition comparisonCondition, EvaluationContext context, String configSalt, String contextSalt, EvaluateLogger evaluateLogger) {
        //TODO evalLogger CC eval is happening
         if(context.getUser() == null){
             // evaluateLogger "Skipping % options because the User Object is missing."
             //TODO isUserMissing in context? check pyhton
             return false;
         }

        String comparisonAttribute = comparisonCondition.getComparisonAttribute();
        Comparator comparator = Comparator.fromId(comparisonCondition.getComparator());
        String userValue = context.getUser().getAttribute(comparisonAttribute);

        //TODO Check if all value available. User missing is separated handle in every Condition checks? cc/sc/pfc
        //TODO what if CV value is not the right one for the comparator?  How to hand,e CV missing? etc.
//        if (comparisonValue == null || comparisonValue.isEmpty() ||
        if (userValue == null || userValue.isEmpty()) {
            //evaluateLogger.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue);
            return false;
        }

        //TODO comparator null? error?
        //TODO in case of exception catch return false. is this oK?
        if(comparator == null){
            return false;
            //TODO do we log the comparator is invalid somewhere?
//            default:
//                throw  new RuntimeException("Comparison operator is invalid.");
        }
        switch (comparator) {
            //TODO log match should be handled on return and just for the TR?
            // evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, containsValues, value);
            case CONTAINS_ANY_OF:
                List<String> containsValues = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                containsValues.replaceAll(String::trim);
                containsValues.removeAll(Arrays.asList(null, ""));
                for (String containsValue : containsValues) {
                    if (userValue.contains(containsValue))
                        return true;
                }
                return false;
            case NOT_CONTAINS_ANY_OF:
                List<String> notContainsValues = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                notContainsValues.replaceAll(String::trim);
                notContainsValues.removeAll(Arrays.asList(null, ""));
                for (String notcontainsValue : notContainsValues) {
                    if (userValue.contains(notcontainsValue))
                        return false;
                }
                return true;
            case SEMVER_IS_ONE_OF:
            case SEMVER_IS_NOT_ONE_OF:
                List<String> inSemVerValues = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
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
                    String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, inSemVerValues, e);
                    this.logger.warn(0, message);
                    return false;
                }
            case SEMVER_LESS:
            case SEMVER_LESS_EQULAS:
            case SEMVER_GREATER:
            case SEMVER_GREATER_EQUALS:
                try {
                    Version cmpUserVersion = Version.parseVersion(userValue.trim(), true);
                    String comparisonValue = comparisonCondition.getStringValue();
                    Version matchValue = Version.parseVersion(comparisonValue.trim(), true);
                    return (Comparator.SEMVER_LESS.equals(comparator) && cmpUserVersion.isLowerThan(matchValue)) ||
                            (Comparator.SEMVER_LESS_EQULAS.equals(comparator) && cmpUserVersion.compareTo(matchValue) <= 0) ||
                            (Comparator.SEMVER_GREATER.equals(comparator) && cmpUserVersion.isGreaterThan(matchValue)) ||
                            (Comparator.SEMVER_GREATER_EQUALS.equals(comparator) && cmpUserVersion.compareTo(matchValue) >= 0);
                } catch (Exception e) {
                    String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonCondition.getStringValue(), e);
                    this.logger.warn(0, message);
                    return false;
                }
            case NUMBER_EQUALS:
            case NUMBER_NOT_EQUALS:
            case NUMBER_LESS:
            case NUMBER_LESS_EQUALS:
            case NUMBER_GREATER:
            case NUMBER_GREATER_EQUALS:
                try {
                    Double userDoubleValue = Double.parseDouble(userValue.trim().replaceAll(",", "."));
                    Double comparisonDoubleValue = comparisonCondition.getDoubleValue();

                    return (Comparator.NUMBER_EQUALS.equals(comparator) && userDoubleValue.equals(comparisonDoubleValue)) ||
                            (Comparator.NUMBER_NOT_EQUALS.equals(comparator) && !userDoubleValue.equals(comparisonDoubleValue)) ||
                            (Comparator.NUMBER_LESS.equals(comparator) && userDoubleValue < comparisonDoubleValue) ||
                            (Comparator.NUMBER_LESS_EQUALS.equals(comparator) && userDoubleValue <= comparisonDoubleValue) ||
                            (Comparator.NUMBER_GREATER.equals(comparator) && userDoubleValue > comparisonDoubleValue) ||
                            (Comparator.NUMBER_GREATER_EQUALS.equals(comparator) && userDoubleValue >= comparisonDoubleValue);
                } catch (NumberFormatException e) {
                    String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonCondition.getDoubleValue(), e);
                    this.logger.warn(0, message);
                    return false;
                }
            case SENSITIVE_IS_ONE_OF:
                //TODO salt error handle
                List<String> inValuesSensitive = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                inValuesSensitive.replaceAll(String::trim);
                inValuesSensitive.removeAll(Arrays.asList(null, ""));
                String hashValueOne = getSaltedUserValue(userValue, configSalt, contextSalt);
                return inValuesSensitive.contains(hashValueOne);
            case SENSITIVE_IS_NOT_ONE_OF:
                //TODO add salt and salt error handle
                List<String> notInValuesSensitive = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                notInValuesSensitive.replaceAll(String::trim);
                notInValuesSensitive.removeAll(Arrays.asList(null, ""));
                String hashValueNotOne = getSaltedUserValue(userValue, configSalt, contextSalt);
                return !notInValuesSensitive.contains(hashValueNotOne);
            case DATE_BEFORE:
            case DATE_AFTER:
                try {
                    double userDoubleValue = Double.parseDouble(userValue.trim().replaceAll(",", "."));
                    Double comparisonDoubleValue = comparisonCondition.getDoubleValue();
                    return (Comparator.DATE_BEFORE.equals(comparator) && userDoubleValue < comparisonDoubleValue) ||
                            (Comparator.DATE_AFTER.equals(comparator) && userDoubleValue > comparisonDoubleValue);
                } catch (NumberFormatException e) {
                    //TODO add new error handling to Date '{userAttributeValue}' is not a valid Unix timestamp (number of seconds elapsed since Unix epoch)
                    String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonCondition.getDoubleValue(), e);
                    this.logger.warn(0, message);
                    return false;
                }
            case HASHED_EQUALS:
                //TODO add salt and salt error handle
                String hashEquals = getSaltedUserValue(userValue, configSalt, contextSalt);
                return hashEquals.equals(comparisonCondition.getStringValue());
            case HASHED_NOT_EQUALS:
                //TODO add salt and salt error handle
                String hashNotEquals = getSaltedUserValue(userValue, configSalt, contextSalt);
                return !hashNotEquals.equals(comparisonCondition.getStringValue());
            case HASHED_STARTS_WITH:
            case HASHED_ENDS_WITH:
            case HASHED_NOT_STARTS_WITH:
            case HASHED_NOT_ENDS_WITH:
                //TODO add salt and salt error handle
                List<String> withValues = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                withValues.replaceAll(String::trim);
                withValues.removeAll(Arrays.asList(null, ""));
                boolean foundEqual = false;
                for (String comparisonValueHashedStartsEnds : withValues) {
                    int indexOf = comparisonValueHashedStartsEnds.indexOf("_");
                    //TODO is it false or skip
                    if (indexOf <= 0) {
                        return false;
                    }
                    String comparedTextLength = comparisonValueHashedStartsEnds.substring(0, indexOf);
                    try {
                        int comparedTextLengthInt = Integer.parseInt(comparedTextLength);
                        if (userValue.length() < comparedTextLengthInt) {
                            continue;
                        }
                        String comparisonHashValue = comparisonValueHashedStartsEnds.substring(indexOf + 1);
                        if (comparisonHashValue.isEmpty()) {
                            return false;
                        }
                        String userValueSubString;
                        if (Comparator.HASHED_STARTS_WITH.equals(comparator) || Comparator.HASHED_NOT_STARTS_WITH.equals(comparator)) {
                            userValueSubString = userValue.substring(0, comparedTextLengthInt);
                        } else { //HASHED_ENDS_WITH
                            userValueSubString = userValue.substring(userValue.length() - comparedTextLengthInt);
                        }
                        String hashUserValueSub = getSaltedUserValue(userValueSubString, configSalt, contextSalt);
                        if(hashUserValueSub.equals(comparisonHashValue)){
                            foundEqual = true;
                        }
                    } catch (NumberFormatException e) {
                        String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValueHashedStartsEnds, e);
                        this.logger.warn(0, message);
                        return false;
                    }
                }
                if (Comparator.HASHED_NOT_STARTS_WITH.equals(comparator) || Comparator.HASHED_NOT_ENDS_WITH.equals(comparator)) {
                    return !foundEqual;
                }
                return foundEqual;
            case HASHED_ARRAY_CONTAINS:
                //TODO add salt and salt error handle
                List<String> containsHashedValues = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                String[] userCSVContainsHashSplit = userValue.split(",");
                for (String userValueSlice : userCSVContainsHashSplit) {
                    String userValueSliceHash = getSaltedUserValue(userValueSlice.trim(), configSalt, contextSalt);
                    if (containsHashedValues.contains(userValueSliceHash)) {
                        return true;
                    }
                }
                return false;
            case HASHED_ARRAY_NOT_CONTAINS:
                //TODO add salt and salt error handle
                List<String> notContainsHashedValues = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
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
        }
        return false;
    }

    private static String getSaltedUserValue(String userValue, String configJsonSalt, String contextSalt) {
        return new String(Hex.encodeHex(DigestUtils.sha256(userValue + configJsonSalt + contextSalt)));
    }

    private boolean evaluateSegmentCondition(SegmentCondition segmentCondition, EvaluationContext context, String configSalt, Segment[] segments, EvaluateLogger evaluateLogger) {
        if(context.getUser() == null){
            // evaluateLogger "Skipping % options because the User Object is missing."
            //TODO isUserMissing in context? check pyhton
            return false;
        }
        //TODO get index.
        //TODO get segment . error if invalid
        int segmentIndex = segmentCondition.getSegmentIndex();
        if(segmentIndex >= segments.length ){
            //TODO log invalid segment
            return false;
        }
        Segment segment = segments[segmentIndex];
        String segmentName = segment.getName();
        if(segmentName == null || segmentName.isEmpty()){
            //TODO log segment name is missing
            return false;
        }
        //TODO add logging
        boolean segmentRulesResult = false;
        for (ComparisonCondition comparisonCondition : segment.getSegmentRules()) {
            segmentRulesResult = evaluateComparisonCondition(comparisonCondition, context, configSalt, segmentName, evaluateLogger);
            //this is an AND if one false we can start the evaluation on the segmentComperator
            if(!segmentRulesResult){
                break;
            }
        }
        SegmentComparator segmentComparator = SegmentComparator.fromId(segmentCondition.getSegmentComparator());

        if(SegmentComparator.IS_IN_SEGMENT.equals(segmentComparator)){
            return segmentRulesResult;
        }else {
            return !segmentRulesResult;
        }
    }

    private boolean evaluatePrerequisiteFlagCondition(PrerequisiteFlagCondition prerequisiteFlagCondition, EvaluationContext context, EvaluateLogger evaluateLogger) {
        //TODO add logger evaluateLogger
        String prerequisiteFlagKey = prerequisiteFlagCondition.getPrerequisiteFlagKey();
        Setting prerequsiteFlagSetting = context.getSettings().get(prerequisiteFlagKey);
        if(prerequisiteFlagKey == null || prerequisiteFlagKey.isEmpty() || prerequsiteFlagSetting == null){
            // TODO Log error
            return false;
        }
        if(context.getVisitedKeys().contains(prerequisiteFlagKey)){
            //TODO log eval , return error message?
            //TODO log warning circular
            // logger.warn();
            return false;
        }

        EvaluationResult evaluateResult = evaluate(prerequsiteFlagSetting, prerequisiteFlagKey, context.getUser(), context.getVisitedKeys(), context.getSettings(), evaluateLogger);
        if(evaluateResult.value == null) {
            //TODO log some error
            return false;
        }
        PrerequisiteComparator prerequisiteComparator = PrerequisiteComparator.fromId(prerequisiteFlagCondition.getPrerequisiteComparator());
        SettingsValue conditionValue = prerequisiteFlagCondition.getValue();
        if(PrerequisiteComparator.EQUALS.equals(prerequisiteComparator)){
            return conditionValue.equals(evaluateResult.value);
        } else {
            return !conditionValue.equals(evaluateResult.value);
        }
    }

    private EvaluationResult evaluateTargetingRules(Setting setting, EvaluationContext context, EvaluateLogger evaluateLogger) {
        //TODO evaluation context should be added?
        //TODO logger eval targeting rules apply first ....

        for (TargetingRule rule : setting.getTargetingRules()) {

            if (!evaluateConditions(rule.getConditions(), context, setting.getConfigSalt(), setting.getSegments(), evaluateLogger)) {
                continue;
            }
            // Conditions match, if rule.getServedValue() not null. we shuold return as logMatch value from SV
            //if no SV then PO should be aviable
            if (rule.getServedValue() != null) {
                return new EvaluationResult(rule.getServedValue().getValue(), rule.getServedValue().getVariationId(), rule, null);
            }
            //if (PO.lenght <= 0) error case no SV and no PO
            if (rule.getPercentageOptions() == null || rule.getPercentageOptions().length == 0) {
                //TODO error? log something?
                continue;
            }
            EvaluationResult evaluatePercentageOptionsResult = evaluatePercentageOptions(rule.getPercentageOptions(), setting.getPercentageAttribute(), context, rule, evaluateLogger);
            if(evaluatePercentageOptionsResult == null){
                continue;
            }
            return evaluatePercentageOptionsResult;

        }
        //TODO loogging should be reworked.
        // evaluateLogger.logNoMatch(comparisonAttribute, userValue, comparator, comparisonCondition);
        return null;
    }

    private boolean evaluateConditions(Condition[] conditions, EvaluationContext context, String configSalt, Segment[] segments, EvaluateLogger evaluateLogger) {
        //Conditions are ANDs so if One is not matching return false, if all matching return true
        //TODO rework logging based on changes possibly
        boolean conditionsEvaluationResult = false;
        for (Condition condition : conditions) {
            //TODO log IF, AND based on order

            //TODO Condition, what if condition invalid? more then one condition added or none. rework basic if
            if (condition.getComparisonCondition() != null) {
                conditionsEvaluationResult = evaluateComparisonCondition(condition.getComparisonCondition(), context, configSalt,context.getKey(), evaluateLogger);
            } else if (condition.getSegmentCondition() != null) {
                //TODO evalSC
                conditionsEvaluationResult = evaluateSegmentCondition(condition.getSegmentCondition(), context, configSalt, segments, evaluateLogger);
            } else if (condition.getPrerequisiteFlagCondition() != null) {
                conditionsEvaluationResult = evaluatePrerequisiteFlagCondition(condition.getPrerequisiteFlagCondition(),context, evaluateLogger);
                //TODO here, we should return with value....
            }
            // else throw Some exception here?
            if (!conditionsEvaluationResult) {
                //TODO no match for the TR. LOG and go to the next one?
                //TODO this should be return from a condEvalMethod
                return false;
            }
        }
        return conditionsEvaluationResult;
    }

    private static EvaluationResult evaluatePercentageOptions(PercentageOption[] percentageOptions, String percentageOptionAttribute, EvaluationContext context, TargetingRule parentTargetingRule, EvaluateLogger evaluateLogger) {
        if (context.getUser() == null){
            // evaluateLogger "Skipping % options because the User Object is missing."
            //TODO isUserMissing in context? check pyhton
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
                //TODO log skip because attribute value missing
                return null;
            }
        }
        //TODO log missing Eval % option based on .....
        String hashCandidate = context.getKey() + percentageOptionAttributeValue;
        int scale = 100;
        String hexHash = new String(Hex.encodeHex(DigestUtils.sha1(hashCandidate))).substring(0, 7);
        int longHash = Integer.parseInt(hexHash, 16);
        int scaled = longHash % scale;

        int bucket = 0;
        for (PercentageOption rule : percentageOptions) {

            bucket += rule.getPercentage();
            if (scaled < bucket) {
                evaluateLogger.logPercentageEvaluationReturnValue(rule.getValue().toString());
                return new EvaluationResult(rule.getValue(), rule.getVariationId(), parentTargetingRule, rule);
            }
        }
        return null;
    }

}
