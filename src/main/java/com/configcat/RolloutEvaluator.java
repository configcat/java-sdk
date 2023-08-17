package com.configcat;

import de.skuzzle.semantic.Version;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RolloutEvaluator {
    private final ConfigCatLogger logger;

    public RolloutEvaluator(ConfigCatLogger logger) {
        this.logger = logger;
    }

    public EvaluationResult evaluate(Setting setting, String key, User user) {
        //TODO this  is a huge method, try to make it smaller

        //TODO logger trick implement? in case info? check it
        EvaluateLogger evaluateLogger = new EvaluateLogger(key);

        try {

            if (user == null) {
                //TODO handle missing user logging. based on changes.
                if ((setting.getTargetingRules() != null && setting.getTargetingRules().length > 0) ||
                        (setting.getPercentageOptions() != null && setting.getPercentageOptions().length > 0)) {
                    this.logger.warn(3001, ConfigCatLogMessages.getTargetingIsNotPossible(key));
                }

                evaluateLogger.logReturnValue(setting.getSettingsValue().toString());
                return new EvaluationResult(setting.getSettingsValue(), setting.getVariationId(), null, null);
            }

            evaluateLogger.logUserObject(user);

            if (setting.getTargetingRules() != null) {
                EvaluationResult targetingRulesEvaluationResult = evaluateTargetingRules(setting, user, key, evaluateLogger);
                if (targetingRulesEvaluationResult != null) return targetingRulesEvaluationResult;
            }

            if (setting.getPercentageOptions() != null && setting.getPercentageOptions().length > 0) {
                EvaluationResult percentageOptionsEvaluationResult = evaluatePercentageOptions(setting.getPercentageOptions(), setting.getPercentageAttribute(), key, user, evaluateLogger);
                if (percentageOptionsEvaluationResult != null) return percentageOptionsEvaluationResult;
            }

            evaluateLogger.logReturnValue(setting.getSettingsValue().toString());
            return new EvaluationResult(setting.getSettingsValue(), setting.getVariationId(), null, null);
        } finally {
            this.logger.info(5000, evaluateLogger.toPrint());
        }
    }

    private boolean evaluateComparisonCondition(ComparisonCondition comparisonCondition, User user, String configSalt, String key, EvaluateLogger evaluateLogger){
        String comparisonAttribute = comparisonCondition.getComparisonAttribute();
        Comparator comparator = Comparator.fromId(comparisonCondition.getComparator());
        String userValue = user.getAttribute(comparisonAttribute);


        //TODO Check if all value available. User missing is separated handle in every Condition checks? cc/sc/pfc
        //TODO what if CV value is not the right one for the comparator?  How to hand,e CV missing? etc.
//        if (comparisonValue == null || comparisonValue.isEmpty() ||
                            if(userValue == null || userValue.isEmpty()) {
                        //evaluateLogger.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue);
                        return false;
                    }

        //TODO comparator null? error?
        //TODO in case of exception catch return false. is this oK?
        switch (comparator) {
            //TODO log match should be handled on return and just for the TR?
            // evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, containsValues, value);
            case CONTAINS_ANY_OF:
                List<String> containsValues = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                containsValues.replaceAll(String::trim);
                containsValues.removeAll(Arrays.asList(null, ""));
                for (String containsValue: containsValues) {
                    if (userValue.contains(containsValue))
                        return true;
                }
                return false;
            case NOT_CONTAINS_ANY_OF:
                List<String> notContainsValues = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                notContainsValues.replaceAll(String::trim);
                notContainsValues.removeAll(Arrays.asList(null, ""));
                for (String notcontainsValue: notContainsValues) {
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
                    Version userVersion = Version.parseVersion(userValue, true);
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
                    Version cmpUserVersion = Version.parseVersion(userValue, true);
                    String comparisonValue = comparisonCondition.getStringValue();
                    Version matchValue = Version.parseVersion(comparisonValue.trim(), true);
                    return (Comparator.SEMVER_LESS.equals(comparator)&& cmpUserVersion.isLowerThan(matchValue)) ||
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
                    Double userDoubleValue = Double.parseDouble(userValue.replaceAll(",", "."));
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
                String hashValueOne = getSaltedUserValue(userValue, configSalt, key);
                return inValuesSensitive.contains(hashValueOne);
            case SENSITIVE_IS_NOT_ONE_OF:
                //TODO add salt and salt error handle
                List<String> notInValuesSensitive = new ArrayList<>(Arrays.asList(comparisonCondition.getStringArrayValue()));
                notInValuesSensitive.replaceAll(String::trim);
                notInValuesSensitive.removeAll(Arrays.asList(null, ""));
                String hashValueNotOne = getSaltedUserValue(userValue, configSalt, key);
                return !notInValuesSensitive.contains(hashValueNotOne);
            case DATE_BEFORE:
            case DATE_AFTER:
                try {
                    Double userDoubleValue = Double.parseDouble(userValue.replaceAll(",", "."));
                    Double comparisonDoubleValue = comparisonCondition.getDoubleValue();
                   return (Comparator.DATE_BEFORE.equals(comparator)&& userDoubleValue < comparisonDoubleValue) ||
                            (Comparator.DATE_AFTER.equals(comparator) && userDoubleValue > comparisonDoubleValue);
                } catch (NumberFormatException e) {
                    String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonCondition.getDoubleValue(), e);
                    this.logger.warn(0, message);
                    return false;
                }
            case HASHED_EQUALS:
                //TODO add salt and salt error handle
                String hashEquals = getSaltedUserValue(userValue, configSalt, key);
                return hashEquals.equals(comparisonCondition.getStringValue());
            case HASHED_NOT_EQUALS:
                //TODO add salt and salt error handle
                String hashNotEquals = getSaltedUserValue(userValue, configSalt, key);
                return !hashNotEquals.equals(comparisonCondition.getStringValue());
            case HASHED_STARTS_WITH:
            case HASHED_ENDS_WITH:
                //TODO add salt and salt error handle
                String comparisonValueHashedStartsEnds = comparisonCondition.getStringValue();
                int indexOf = comparisonValueHashedStartsEnds.indexOf("_");
                if(indexOf <= 0){
                    return false;
                }
                String comparedTextLength = comparisonValueHashedStartsEnds.substring(0, indexOf);
                try {
                    int comparedTextLengthInt = Integer.parseInt(comparedTextLength);
                    if(userValue.length() < comparedTextLengthInt){
                        return false;
                    }
                    String comparisonHashValue = comparisonValueHashedStartsEnds.substring(indexOf + 1);
                    if(comparisonHashValue.isEmpty()){
                        return false;
                    }
                    String userValueSubString;
                    if(Comparator.HASHED_STARTS_WITH.equals(comparator)){
                        userValueSubString = userValue.substring(0, comparedTextLengthInt);
                    } else { //HASHED_ENDS_WITH
                        userValueSubString = userValue.substring(userValue.length() - comparedTextLengthInt);
                    }
                    String hashUserValueSub = getSaltedUserValue(userValueSubString, configSalt, key);
                    return hashUserValueSub.equals(comparisonHashValue);
                } catch (NumberFormatException e) {
                    String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValueHashedStartsEnds, e);
                    this.logger.warn(0, message);
                    return false;
                }
            case HASHED_ARRAY_CONTAINS:
                //TODO add salt and salt error handle
                String[] userCSVContainsHashSplit = userValue.split(",");
                if(userCSVContainsHashSplit.length == 0){
                    return false;
                }
                for (String userValueSlice: userCSVContainsHashSplit) {
                    String userValueSliceHash = getSaltedUserValue(userValueSlice, configSalt, key);
                    if (userValueSliceHash.equals(comparisonCondition.getStringValue())) {
                    return true;                    }
                }
                return false;
            case HASHED_ARRAY_NOT_CONTAINS:
                //TODO add salt and salt error handle
                String[] userCSVNotContainsHashSplit = userValue.split(",");
                if(userCSVNotContainsHashSplit.length == 0){
                    return false;
                }
                boolean containsFlag = false;
                for (String userValueSlice: userCSVNotContainsHashSplit) {
                    String userValueSliceHash = getSaltedUserValue(userValueSlice, configSalt, key);
                    if (userValueSliceHash.equals(comparisonCondition.getStringValue())) {
                        containsFlag = true;
                    }
                }
                return !containsFlag;

        }
        return true;
    }

    @NotNull
    private static String getSaltedUserValue(String userValue, String configJsonSalt, String key) {
        return new String(Hex.encodeHex(DigestUtils.sha256(userValue + configJsonSalt + key)));
    }

    private boolean evaluateSegmentCondition(SegmentCondition segmentCondition){
        //TODO implement
        return true;
    }

    private boolean evaluatePrerequisiteFlagCondition(PrerequisiteFlagCondition prerequisiteFlagCondition){
        //TODO implement
        return true;
    }

    @Nullable
    private EvaluationResult evaluateTargetingRules(Setting setting, User user, String key, EvaluateLogger evaluateLogger) {
        //TODO evaluation context should be added?
        //TODO logger eval targeting rules apply first ....

        for (TargetingRule rule : setting.getTargetingRules()) {

            //TODO
            if(!evaluateConditions(rule.getConditions(), user, setting.getConfigSalt(), key, evaluateLogger)){
                continue;
            }
            // Conditions match, if rule.getServedValue() not null. we shuold return as logMatch value from SV
            //if no SV then PO should be aviable
            if(rule.getServedValue() != null){
                return new EvaluationResult(rule.getServedValue().getValue(), rule.getServedValue().getVariationId(), rule, null);
            }
            //if (PO.lenght <= 0) error case no SV and no PO
            if(rule.getPercentageOptions() == null || rule.getPercentageOptions().length == 0){
                //TODO error? log something?
                continue;
            }
            //TODO can be percentage option here?
            //rule.getPercentageOption();
            //should this be the same evalPO? rework to ony pass the PO[]?
            //TODO here the result should contaion the tr as weLL?
            return evaluatePercentageOptions(rule.getPercentageOptions(), setting.getPercentageAttribute(), key, user, evaluateLogger );
        }
        //TODO loogging should be reworked.
        // evaluateLogger.logNoMatch(comparisonAttribute, userValue, comparator, comparisonCondition);
        return null;
    }

    private boolean evaluateConditions(Condition[] conditions, User user, String configSalt, String key, EvaluateLogger evaluateLogger) {
        //Conditions are ANDs so if One is not matching return false, if all matching return true
        //TODO rework logging based on changes possibly
        boolean conditionsEvaluationResult = false;
        for (Condition condition: conditions) {
            //TODO log IF, AND based on order

            //TODO Condition, what if condition invalid? more then one condition added or none. rework basic if
            if(condition.getComparisonCondition() != null){
                conditionsEvaluationResult = evaluateComparisonCondition(condition.getComparisonCondition(), user,configSalt, key, evaluateLogger);
            } else if(condition.getSegmentCondition() != null){
                //TODO evalSC
                conditionsEvaluationResult = evaluateSegmentCondition(condition.getSegmentCondition());
            }else if(condition.getPrerequisiteFlagCondition() != null){
                conditionsEvaluationResult = evaluatePrerequisiteFlagCondition(condition.getPrerequisiteFlagCondition());
                //TODO evalPFC
            }
            // else throw Some exception here?
           if(!conditionsEvaluationResult){
               //TODO no match for the TR. LOG and go to the next one?
               //TODO this should be return from a condEvalMethod
                return false;
           }
        }
        return conditionsEvaluationResult;
    }

    @Nullable
    private static EvaluationResult evaluatePercentageOptions(PercentageOption[] percentageOptions, String percentageOptionAttribute, String key, User user, EvaluateLogger evaluateLogger) {
        //TODO if user missing? based on .net skipp should be logged here
        //TODO setting.getPercentageAttribute()?
        String percentageOptionAttributeValue;
        String percentageOptionAttributeName = percentageOptionAttribute;
        if(percentageOptionAttributeName == null || percentageOptionAttributeName.isEmpty()){
            //TODO add some const for the "Identifier"
            percentageOptionAttributeName = "Identifier";
            percentageOptionAttributeValue = user.getIdentifier();
        }else{
            percentageOptionAttributeValue = user.getAttribute(percentageOptionAttributeName);
            if(percentageOptionAttributeValue == null){
                //TODO log skip beacuse atribute value missing
                //TODO return with default?
                return null;
            }
        }
        //TODO log misisng Evalu % option based on .....
        //TODO salt must be added?
        String hashCandidate = key + percentageOptionAttributeValue;
        int scale = 100;
        String hexHash =  new String(Hex.encodeHex(DigestUtils.sha1(hashCandidate))).substring(0, 7);
        int longHash = Integer.parseInt(hexHash, 16);
        int scaled = longHash % scale;

        int bucket = 0;
        for (PercentageOption rule : percentageOptions) {

            bucket += rule.getPercentage();
            if (scaled < bucket) {
                evaluateLogger.logPercentageEvaluationReturnValue(rule.getValue().toString());
                return new EvaluationResult(rule.getValue(), rule.getVariationId(), null, rule);
            }
        }
        //TODO log when options % not 100? so we don't need a return here?
        return null;
    }

}
