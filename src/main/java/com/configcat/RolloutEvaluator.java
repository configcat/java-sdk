package com.configcat;

import com.google.gson.JsonElement;
import de.skuzzle.semantic.Version;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.binary.Hex;

import java.util.*;

class RolloutEvaluator {
    private static final String[] COMPARATOR_TEXTS = new String[]{
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

    public Map.Entry<JsonElement, String> evaluate(Setting setting, String key, User user) {
        LogEntries logEntries = new LogEntries();
        logEntries.add("Evaluating getValue(" + key + ").");

        try {

            if (user == null) {
                if ((setting.rolloutRules != null && setting.rolloutRules.length > 0) ||
                        (setting.percentageItems != null && setting.percentageItems.length > 0)) {
                    this.logger.warn("UserObject missing! You should pass a UserObject to getValue() in order to make targeting work properly. Read more: https://configcat.com/docs/advanced/user-object.");
                }

                logEntries.add("Returning " + setting.value + ".");
                return new AbstractMap.SimpleEntry<>(setting.value, setting.variationId);
            }

            logEntries.add("User object: " + user + "");
            if (setting.rolloutRules != null) {
                for (RolloutRule rule : setting.rolloutRules) {

                    String comparisonAttribute = rule.comparisonAttribute;
                    String comparisonValue = rule.comparisonValue;
                    int comparator = rule.comparator;
                    JsonElement value = rule.value;
                    String variationId = rule.variationId;
                    String userValue = user.getAttribute(comparisonAttribute);

                    if (comparisonValue == null || comparisonValue.isEmpty() ||
                            userValue == null || userValue.isEmpty()) {
                        logEntries.add(this.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue));
                        continue;
                    }

                    switch (comparator) {
                        //IS ONE OF
                        case 0:
                            List<String> inValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            inValues.replaceAll(String::trim);
                            inValues.removeAll(Arrays.asList(null, ""));
                            if (inValues.contains(userValue)) {
                                logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                return new AbstractMap.SimpleEntry<>(value, variationId);
                            }
                            break;
                        //IS NOT ONE OF
                        case 1:
                            List<String> notInValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            notInValues.replaceAll(String::trim);
                            notInValues.removeAll(Arrays.asList(null, ""));
                            if (!notInValues.contains(userValue)) {
                                logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                return new AbstractMap.SimpleEntry<>(value, variationId);
                            }
                            break;
                        //CONTAINS
                        case 2:
                            if (userValue.contains(comparisonValue)) {
                                logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                return new AbstractMap.SimpleEntry<>(value, variationId);
                            }
                            break;
                        //DOES NOT CONTAIN
                        case 3:
                            if (!userValue.contains(comparisonValue)) {
                                logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                return new AbstractMap.SimpleEntry<>(value, variationId);
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
                                    logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                    return new AbstractMap.SimpleEntry<>(value, variationId);
                                }
                            } catch (Exception e) {
                                logEntries.add(this.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e));
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
                                    logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                    return new AbstractMap.SimpleEntry<>(value, variationId);
                                }
                            } catch (Exception e) {
                                logEntries.add(this.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e));
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
                                    logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                    return new AbstractMap.SimpleEntry<>(value, variationId);
                                }
                            } catch (NumberFormatException e) {
                                logEntries.add(this.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e));
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
                                logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                return new AbstractMap.SimpleEntry<>(value, variationId);
                            }
                            break;
                        //IS NOT ONE OF (Sensitive)
                        case 17:
                            List<String> notInValuesSensitive = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                            notInValuesSensitive.replaceAll(String::trim);
                            notInValuesSensitive.removeAll(Arrays.asList(null, ""));
                            String hashValueNotOne = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                            if (!notInValuesSensitive.contains(hashValueNotOne)) {
                                logEntries.add(this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value));
                                return new AbstractMap.SimpleEntry<>(value, variationId);
                            }
                            break;
                    }
                    logEntries.add(this.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue));
                }
            }

            if (setting.percentageItems != null && setting.percentageItems.length > 0) {
                String hashCandidate = key + user.getIdentifier();
                int scale = 100;
                String hexHash = new String(Hex.encodeHex(DigestUtils.sha1(hashCandidate))).substring(0, 7);
                int longHash = Integer.parseInt(hexHash, 16);
                int scaled = longHash % scale;

                int bucket = 0;
                for (RolloutPercentageItem rule : setting.percentageItems) {

                    bucket += rule.percentage;
                    if (scaled < bucket) {
                        logEntries.add("Evaluating % options. Returning " + rule.value + ".");
                        return new AbstractMap.SimpleEntry<>(rule.value, rule.variationId);
                    }
                }
            }

            logEntries.add("Returning " + setting.value + ".");
            return new AbstractMap.SimpleEntry<>(setting.value, setting.variationId);
        } finally {
            this.logger.info(logEntries.toPrint());
        }
    }

    private String logMatch(String comparisonAttribute, String userValue, int comparator, String comparisonValue, Object value) {
        return "Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + COMPARATOR_TEXTS[comparator] + "] [" + comparisonValue + "] => match, returning: " + value + "";
    }

    private String logNoMatch(String comparisonAttribute, String userValue, int comparator, String comparisonValue) {
        return "Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + COMPARATOR_TEXTS[comparator] + "] [" + comparisonValue + "] => no match";
    }

    private String logFormatError(String comparisonAttribute, String userValue, int comparator, String comparisonValue, Exception exception) {
        String message = "Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + COMPARATOR_TEXTS[comparator] + "] [" + comparisonValue + "] => SKIP rule. Validation error: " + exception + "";
        this.logger.warn(message);
        return message;
    }

    static class LogEntries {
        private final List<String> entries = new ArrayList<>();

        public void add(String entry) {
            this.entries.add(entry);
        }

        public String toPrint() {
            return String.join(System.lineSeparator(), this.entries);
        }
    }
}
