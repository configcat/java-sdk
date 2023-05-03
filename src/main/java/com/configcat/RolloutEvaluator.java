package com.configcat;

import com.configcat.log.ConfigCatLogMessages;
import com.configcat.log.ConfigCatLogger;
import com.google.gson.JsonElement;
import de.skuzzle.semantic.Version;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RolloutEvaluator {
    protected static final String[] COMPARATOR_TEXTS = new String[]{
            "IS ONE OF",
            "IS NOT ONE OF",
            "CONTAINS",
            "DOES NOT CONTAIN",
            "IS ONE OF (SemVer)",
            "IS NOT ONE OF (SemVer)",
            "< (SemVer)",
            "<= (SemVer)",
            "> (SemVer)",
            ">= (SemVer)",
            "= (Number)",
            "<> (Number)",
            "< (Number)",
            "<= (Number)",
            "> (Number)",
            ">= (Number)",
            "IS ONE OF (Sensitive)",
            "IS NOT ONE OF (Sensitive)"
    };

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
                    int comparator = rule.getComparator();
                    JsonElement value = rule.getValue();
                    String variationId = rule.getVariationId();
                    String userValue = user.getAttribute(comparisonAttribute);

                    if (comparisonValue == null || comparisonValue.isEmpty() ||
                            userValue == null || userValue.isEmpty()) {
                        evaluateLogger.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue);
                        continue;
                    }

                    switch (comparator) {
                        //IS ONE OF
                        case 0:
                            List<String> inValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            inValues.replaceAll(String::trim);
                            inValues.removeAll(Arrays.asList(null, ""));
                            if (inValues.contains(userValue)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        //IS NOT ONE OF
                        case 1:
                            List<String> notInValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            notInValues.replaceAll(String::trim);
                            notInValues.removeAll(Arrays.asList(null, ""));
                            if (!notInValues.contains(userValue)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        //CONTAINS
                        case 2:
                            if (userValue.contains(comparisonValue)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        //DOES NOT CONTAIN
                        case 3:
                            if (!userValue.contains(comparisonValue)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        //IS ONE OF, IS NOT ONE OF (SemVer)
                        case 4:
                        case 5:
                            List<String> inSemVerValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            inSemVerValues.replaceAll(String::trim);
                            inSemVerValues.removeAll(Arrays.asList(null, ""));
                            try {
                                Version userVersion = Version.parseVersion(userValue, true);
                                boolean matched = false;
                                for (String semVer : inSemVerValues) {
                                    matched = userVersion.compareTo(Version.parseVersion(semVer, true)) == 0 || matched;
                                }

                                if ((matched && comparator == 4) || (!matched && comparator == 5)) {
                                    evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            } catch (Exception e) {
                                String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                                this.logger.warn(0, message);
                                continue;
                            }
                            break;
                        //LESS THAN, LESS THAN OR EQUALS TO, GREATER THAN, GREATER THAN OR EQUALS TO (SemVer)
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                            try {
                                Version cmpUserVersion = Version.parseVersion(userValue, true);
                                Version matchValue = Version.parseVersion(comparisonValue.trim(), true);
                                if ((comparator == 6 && cmpUserVersion.isLowerThan(matchValue)) ||
                                        (comparator == 7 && cmpUserVersion.compareTo(matchValue) <= 0) ||
                                        (comparator == 8 && cmpUserVersion.isGreaterThan(matchValue)) ||
                                        (comparator == 9 && cmpUserVersion.compareTo(matchValue) >= 0)) {
                                    evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            } catch (Exception e) {
                                String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                                this.logger.warn(0, message);
                                continue;
                            }
                            break;
                        //LESS THAN, LESS THAN OR EQUALS TO, GREATER THAN, GREATER THAN OR EQUALS TO (SemVer)
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                            try {
                                Double userDoubleValue = Double.parseDouble(userValue.replaceAll(",", "."));
                                Double comparisonDoubleValue = Double.parseDouble(comparisonValue.replaceAll(",", "."));

                                if ((comparator == 10 && userDoubleValue.equals(comparisonDoubleValue)) ||
                                        (comparator == 11 && !userDoubleValue.equals(comparisonDoubleValue)) ||
                                        (comparator == 12 && userDoubleValue < comparisonDoubleValue) ||
                                        (comparator == 13 && userDoubleValue <= comparisonDoubleValue) ||
                                        (comparator == 14 && userDoubleValue > comparisonDoubleValue) ||
                                        (comparator == 15 && userDoubleValue >= comparisonDoubleValue)) {
                                    evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                    return new EvaluationResult(value, variationId, rule, null);
                                }
                            } catch (NumberFormatException e) {
                                String message = evaluateLogger.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                                this.logger.warn(0, message);
                                continue;
                            }
                            break;
                        //IS ONE OF (Sensitive)
                        case 16:
                            List<String> inValuesSensitive = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            inValuesSensitive.replaceAll(String::trim);
                            inValuesSensitive.removeAll(Arrays.asList(null, ""));
                            String hashValueOne = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                            if (inValuesSensitive.contains(hashValueOne)) {
                                evaluateLogger.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                                return new EvaluationResult(value, variationId, rule, null);
                            }
                            break;
                        //IS NOT ONE OF (Sensitive)
                        case 17:
                            List<String> notInValuesSensitive = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            notInValuesSensitive.replaceAll(String::trim);
                            notInValuesSensitive.removeAll(Arrays.asList(null, ""));
                            String hashValueNotOne = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                            if (!notInValuesSensitive.contains(hashValueNotOne)) {
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
