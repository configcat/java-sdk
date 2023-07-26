package com.configcat;

import com.google.gson.JsonElement;
import de.skuzzle.semantic.Version;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;

class RolloutEvaluator {
    private final ConfigCatLogger logger;

    public RolloutEvaluator(ConfigCatLogger logger) {
        this.logger = logger;
    }

    public EvaluationResult evaluate(Setting setting, String key, User user) {
        EvaluateLogger evaluateLogger = new EvaluateLogger(key);

        try {

            if (user == null) {
                if ((setting.getRolloutRules() != null && setting.getRolloutRules().length > 0) ||
                        (setting.getPercentageItems() != null && setting.getPercentageItems().length > 0)) {
                    this.logger.warn(3001, ConfigCatLogMessages.getTargetingIsNotPossible(key));
                }

                evaluateLogger.logReturnValue(setting.getValue().toString());
                return new EvaluationResult(setting.getValue(), setting.getVariationId(), null, null);
            }

            evaluateLogger.logUserObject(user);
            if (setting.getRolloutRules() != null) {
                for (RolloutRule rule : setting.getRolloutRules()) {

                    String comparisonAttribute = rule.getComparisonAttribute();
                    String comparisonValue = rule.getComparisonValue();
                    Comparator comparator = Comparator.fromId(rule.getComparator());
                    JsonElement value = rule.getValue();
                    String variationId = rule.getVariationId();
                    String userValue = user.getAttribute(comparisonAttribute);

                    if (comparisonValue == null || comparisonValue.isEmpty() ||
                            userValue == null || userValue.isEmpty()) {
                        evaluateLogger.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue);
                        continue;
                    }

                    //TODO comparator null? error?
                    switch (comparator) {
                        case CONTAINS:
                            List<String> containsValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            containsValues.replaceAll(String::trim);
                            containsValues.removeAll(Arrays.asList(null, ""));
                            if (containsValues.contains(userValue)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        case DOES_NOT_CONTAIN:
                            List<String> notContainsValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            notContainsValues.replaceAll(String::trim);
                            notContainsValues.removeAll(Arrays.asList(null, ""));
                            if (!notContainsValues.contains(userValue)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }

                            break;
                        case SEMVER_IS_ONE_OF:
                        case SEMVER_IS_NOT_ONE_OF:
                            List<String> inSemVerValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            inSemVerValues.replaceAll(String::trim);
                            inSemVerValues.removeAll(Arrays.asList(null, ""));
                            try {
                                Version userVersion = Version.parseVersion(userValue, true);
                                boolean matched = false;
                                for (String semVer : inSemVerValues) {
                                    matched = userVersion.compareTo(Version.parseVersion(semVer, true)) == 0 || matched;
                                }

                                if ((matched && Comparator.SEMVER_IS_ONE_OF.equals(comparator)) || (!matched && Comparator.SEMVER_IS_NOT_ONE_OF.equals(comparator))) {
                                    evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            } catch (Exception e) {
                                String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                                this.logger.warn(0, message);
                                continue;
                            }
                            break;
                        case SEMVER_LESS:
                        case SEMVER_LESS_EQULAS:
                        case SEMVER_GREATER:
                        case SEMVER_GREATER_EQUALS:
                            try {
                                Version cmpUserVersion = Version.parseVersion(userValue, true);
                                Version matchValue = Version.parseVersion(comparisonValue.trim(), true);
                                if ((Comparator.SEMVER_LESS.equals(comparator)&& cmpUserVersion.isLowerThan(matchValue)) ||
                                        (Comparator.SEMVER_LESS_EQULAS.equals(comparator) && cmpUserVersion.compareTo(matchValue) <= 0) ||
                                        (Comparator.SEMVER_GREATER.equals(comparator) && cmpUserVersion.isGreaterThan(matchValue)) ||
                                        (Comparator.SEMVER_GREATER_EQUALS.equals(comparator) && cmpUserVersion.compareTo(matchValue) >= 0)) {
                                    evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            } catch (Exception e) {
                                String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                                this.logger.warn(0, message);
                                continue;
                            }
                            break;
                        case NUMBER_EQUALS:
                        case NUMBER_NOT_EQUALS:
                        case NUMBER_LESS:
                        case NUMBER_LESS_EQUALS:
                        case NUMBER_GREATER:
                        case NUMBER_GREATER_EQUALS:
                            try {
                                Double userDoubleValue = Double.parseDouble(userValue.replaceAll(",", "."));
                                Double comparisonDoubleValue = Double.parseDouble(comparisonValue.replaceAll(",", "."));

                                if ((Comparator.NUMBER_EQUALS.equals(comparator) && userDoubleValue.equals(comparisonDoubleValue)) ||
                                        (Comparator.NUMBER_NOT_EQUALS.equals(comparator) && !userDoubleValue.equals(comparisonDoubleValue)) ||
                                        (Comparator.NUMBER_LESS.equals(comparator) && userDoubleValue < comparisonDoubleValue) ||
                                        (Comparator.NUMBER_LESS_EQUALS.equals(comparator) && userDoubleValue <= comparisonDoubleValue) ||
                                        (Comparator.NUMBER_GREATER.equals(comparator) && userDoubleValue > comparisonDoubleValue) ||
                                        (Comparator.NUMBER_GREATER_EQUALS.equals(comparator) && userDoubleValue >= comparisonDoubleValue)) {
                                    evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            } catch (NumberFormatException e) {
                                String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                                this.logger.warn(0, message);
                                continue;
                            }
                            break;
                        case SENSITIVE_IS_ONE_OF:
                            //TODO add salt and salt error handle
                            List<String> inValuesSensitive = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            inValuesSensitive.replaceAll(String::trim);
                            inValuesSensitive.removeAll(Arrays.asList(null, ""));
                            String hashValueOne = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                            if (inValuesSensitive.contains(hashValueOne)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        case SENSITIVE_IS_NOT_ONE_OF:
                            //TODO add salt and salt error handle
                            List<String> notInValuesSensitive = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            notInValuesSensitive.replaceAll(String::trim);
                            notInValuesSensitive.removeAll(Arrays.asList(null, ""));
                            String hashValueNotOne = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                            if (!notInValuesSensitive.contains(hashValueNotOne)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        case DATE_BEFORE:
                        case DATE_AFTER:
                            try {
                                Double userDoubleValue = Double.parseDouble(userValue.replaceAll(",", "."));
                                Double comparisonDoubleValue = Double.parseDouble(comparisonValue.replaceAll(",", "."));
                                if ((Comparator.DATE_BEFORE.equals(comparator)&& userDoubleValue < comparisonDoubleValue) ||
                                        (Comparator.DATE_AFTER.equals(comparator) && userDoubleValue > comparisonDoubleValue)){
                                    evaluateLogger.logMatchDate(comparisonAttribute, userDoubleValue, comparator, comparisonDoubleValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            } catch (NumberFormatException e) {
                                String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                                this.logger.warn(0, message);
                                continue;
                            }
                            break;
                        case HASHED_EQUALS:
                            //TODO add salt and salt error handle
                            String hashEquals = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                            if (hashEquals.equals(comparisonValue)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        case HASHED_NOT_EQUALS:
                            //TODO add salt and salt error handle
                            String hashNotEquals = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                            if (!hashNotEquals.equals(comparisonValue)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        case HASHED_STARTS_WITH:
                        case HASHED_ENDS_WITH:
                            //TODO add salt and salt error handle
                            int indexOf = comparisonValue.indexOf("_");
                            if(indexOf <= 0){
                                continue;
                            }
                            String comparedTextLength = comparisonValue.substring(0, indexOf);
                            try {
                                int comparedTextLengthInt = Integer.parseInt(comparedTextLength);
                                if(userValue.length() < comparedTextLengthInt){
                                    continue;
                                }
                                String comparisonHashValue = comparisonValue.substring(indexOf + 1);
                                if(comparisonHashValue.isEmpty()){
                                    continue;
                                }
                                String userValueSubString;
                                if(Comparator.HASHED_STARTS_WITH.equals(comparator)){
                                    userValueSubString = userValue.substring(0, comparedTextLengthInt);
                                } else { //HASHED_ENDS_WITH
                                    userValueSubString = userValue.substring(userValue.length() - comparedTextLengthInt);
                                }
                                String hashUserValueSub = new String(Hex.encodeHex(DigestUtils.sha1(userValueSubString)));
                                if (hashUserValueSub.equals(comparisonHashValue)) {
                                    evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            } catch (NumberFormatException e) {
                                String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                                this.logger.warn(0, message);
                                continue;
                            }
                            break;
                        case HASHED_ARRAY_CONTAINS:
                            //TODO add salt and salt error handle
                            String[] userCSVContainsHashSplit = userValue.split(",");
                            if(userCSVContainsHashSplit.length == 0){
                                continue;
                            }
                            for (String userValueSlice: userCSVContainsHashSplit) {
                                String userValueSliceHash = new String(Hex.encodeHex(DigestUtils.sha1(userValueSlice)));
                                if (userValueSliceHash.equals(comparisonValue)) {
                                    evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            }
                            break;
                        case HASHED_ARRAY_NOT_CONTAINS:
                            //TODO add salt and salt error handle
                            String[] userCSVNotContainsHashSplit = userValue.split(",");
                            if(userCSVNotContainsHashSplit.length == 0){
                                continue;
                            }
                            boolean containsFlag = false;
                            for (String userValueSlice: userCSVNotContainsHashSplit) {
                                String userValueSliceHash = new String(Hex.encodeHex(DigestUtils.sha1(userValueSlice)));
                                if (userValueSliceHash.equals(comparisonValue)) {
                                    containsFlag = true;
                                }
                            }
                            if(!containsFlag){
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                    }
                    evaluateLogger.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue);
                }
            }

            if (setting.getPercentageItems() != null && setting.getPercentageItems().length > 0) {
                String hashCandidate = key + user.getIdentifier();
                int scale = 100;
                String hexHash = new String(Hex.encodeHex(DigestUtils.sha1(hashCandidate))).substring(0, 7);
                int longHash = Integer.parseInt(hexHash, 16);
                int scaled = longHash % scale;

                int bucket = 0;
                for (PercentageRule rule : setting.getPercentageItems()) {

                    bucket += rule.getPercentage();
                    if (scaled < bucket) {
                        evaluateLogger.logPercentageEvaluationReturnValue(rule.getValue().toString());
                        return new EvaluationResult(rule.getValue(), rule.getVariationId(), null, rule);
                    }
                }
            }

            evaluateLogger.logReturnValue(setting.getValue().toString());
            return new EvaluationResult(setting.getValue(), setting.getVariationId(), null, null);
        } finally {
            this.logger.info(5000, evaluateLogger.toPrint());
        }
    }

}
