package com.configcat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.skuzzle.semantic.Version;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RolloutEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolloutEvaluator.class);

    private String[] COMPARATOR_TEXTS = new String[]{
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

    public JsonElement evaluate(JsonObject json, String key, User user) {
        JsonArray rolloutRules = json.getAsJsonArray("r");
        JsonArray percentageRules = json.getAsJsonArray("p");

        LOGGER.info("Evaluating getValue("+ key +").");

        if(user == null) {
            if(rolloutRules.size() > 0 || percentageRules.size() > 0) {
                LOGGER.warn("UserObject missing! You should pass a UserObject to getValue() in order to make targeting work properly. Read more: https://configcat.com/docs/advanced/user-object.");
            }

            JsonElement result = json.get("v");
            LOGGER.info("Returning "+ result +".");
            return json.get("v");
        }

        for (JsonElement rule: rolloutRules) {
            JsonObject ruleObject = rule.getAsJsonObject();

            String comparisonAttribute = ruleObject.get("a").getAsString();
            String comparisonValue = ruleObject.get("c").getAsString();
            int comparator = ruleObject.get("t").getAsInt();
            JsonElement value = ruleObject.get("v");
            String userValue = user.getAttribute(comparisonAttribute);

            if(comparisonValue == null || comparisonValue.isEmpty() ||
                    userValue == null || userValue.isEmpty()) {
                this.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue);
                continue;
            }

            switch (comparator) {
                //IS ONE OF
                case 0:
                    List<String> inValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                    inValues.replaceAll(String::trim);
                    inValues.removeAll(Arrays.asList(null, ""));
                    if(inValues.contains(userValue)) {
                        this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                        return value;
                    }
                    break;
                //IS NOT ONE OF
                case 1:
                    List<String> notInValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                    notInValues.replaceAll(String::trim);
                    notInValues.removeAll(Arrays.asList(null, ""));
                    if(!notInValues.contains(userValue)) {
                        this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                        return value;
                    }
                    break;
                //CONTAINS
                case 2:
                    if(userValue.contains(comparisonValue)) {
                        this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                        return value;
                    }
                    break;
                //DOES NOT CONTAIN
                case 3:
                    if(!userValue.contains(comparisonValue)) {
                        this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                        return value;
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
                            this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                            return value;
                        }
                    } catch (Exception e) {
                        this.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
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
                        if( (comparator == 6 && cmpUserVersion.isLowerThan(matchValue)) ||
                                (comparator == 7 && cmpUserVersion.compareTo(matchValue) <= 0) ||
                                (comparator == 8 && cmpUserVersion.isGreaterThan(matchValue)) ||
                                (comparator == 9 && cmpUserVersion.compareTo(matchValue) >= 0)) {
                            this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                            return value;
                        }
                    } catch (Exception e) {
                        this.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
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

                        if((comparator == 10 && userDoubleValue.equals(comparisonDoubleValue)) ||
                                (comparator == 11 && !userDoubleValue.equals(comparisonDoubleValue)) ||
                                (comparator == 12 && userDoubleValue < comparisonDoubleValue) ||
                                (comparator == 13 && userDoubleValue <= comparisonDoubleValue) ||
                                (comparator == 14 && userDoubleValue > comparisonDoubleValue) ||
                                (comparator == 15 && userDoubleValue >= comparisonDoubleValue)) {
                            this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                            return value;
                        }
                    } catch (NumberFormatException e) {
                        this.logFormatError(comparisonAttribute, userValue, comparator, comparisonValue, e);
                        continue;
                    }
                    break;
                //IS ONE OF (Sensitive)
                case 16:
                    List<String> inValuesSensitive = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                    inValuesSensitive.replaceAll(String::trim);
                    inValuesSensitive.removeAll(Arrays.asList(null, ""));
                    String hashValueOne = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                    if(inValuesSensitive.contains(hashValueOne)) {
                        this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                        return value;
                    }
                    break;
                //IS NOT ONE OF (Sensitive)
                case 17:
                    List<String> notInValuesSensitive = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                    notInValuesSensitive.replaceAll(String::trim);
                    notInValuesSensitive.removeAll(Arrays.asList(null, ""));
                    String hashValueNotOne = new String(Hex.encodeHex(DigestUtils.sha1(userValue)));
                    if(!notInValuesSensitive.contains(hashValueNotOne)) {
                        this.logMatch(comparisonAttribute, userValue, comparator, comparisonValue, value);
                        return value;
                    }
                    break;
            }
            this.logNoMatch(comparisonAttribute, userValue, comparator, comparisonValue);
        }

        if(percentageRules.size() > 0){
            String hashCandidate = key + user.getIdentifier();
            int scale = 100;
            String hexHash = new String(Hex.encodeHex(DigestUtils.sha1(hashCandidate))).substring(0, 7);
            int longHash = Integer.parseInt(hexHash, 16);
            int scaled = longHash % scale;

            int bucket = 0;
            for (JsonElement rule: percentageRules) {
                JsonObject ruleObject = rule.getAsJsonObject();

                bucket += ruleObject.get("p").getAsInt();
                if(scaled < bucket) {
                    JsonElement result = ruleObject.get("v");
                    LOGGER.info("Evaluating % options. Returning "+ result +".");
                    return result;
                }
            }
        }

        JsonElement result = json.get("v");
        LOGGER.info("Returning "+ result +".");
        return result;
    }

    private void logMatch(String comparisonAttribute, String userValue, int comparator, String comparisonValue, JsonElement value) {
        LOGGER.info("Evaluating rule: ["+comparisonAttribute+":"+ userValue +"] ["+COMPARATOR_TEXTS[comparator]+"] ["+comparisonValue+"] => match, returning: "+value+"");
    }

    private void logNoMatch(String comparisonAttribute, String userValue, int comparator, String comparisonValue) {
        LOGGER.info("Evaluating rule: ["+comparisonAttribute+":"+ userValue +"] ["+COMPARATOR_TEXTS[comparator]+"] ["+comparisonValue+"] => no match");
    }

    private void logFormatError(String comparisonAttribute, String userValue, int comparator, String comparisonValue, Exception exception) {
        LOGGER.warn("Evaluating rule: ["+comparisonAttribute+":"+ userValue +"] ["+COMPARATOR_TEXTS[comparator]+"] ["+comparisonValue+"] => SKIP rule. Validation error: "+exception+"");
    }
}
