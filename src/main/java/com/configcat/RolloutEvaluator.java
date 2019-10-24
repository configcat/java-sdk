package com.configcat;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
            ">= (Number)"
    };

    public JsonElement evaluate(JsonObject json, String key, User user) {
        if(user == null) {
            LOGGER.warn("UserObject missing! You should pass a UserObject to getValue() in order to make targeting work properly. Read more: https://configcat.com/docs/advanced/user-object.");
            return json.get("v");
        }

        JsonArray rolloutRules = json.getAsJsonArray("r");

        for (JsonElement rule: rolloutRules) {
            JsonObject ruleObject = rule.getAsJsonObject();

            String comparisonAttribute = ruleObject.get("a").getAsString();
            String comparisonValue = ruleObject.get("c").getAsString();
            int comparator = ruleObject.get("t").getAsInt();
            JsonElement value = ruleObject.get("v");
            String userValue = user.getAttribute(comparisonAttribute);

            if(comparisonValue == null || comparisonValue.isEmpty() ||
                    userValue == null || userValue.isEmpty())
                continue;

            switch (comparator) {
                //IS ONE OF
                case 0:
                    List<String> inValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                    inValues.replaceAll(String::trim);
                    inValues.removeAll(Arrays.asList(null, ""));
                    if(inValues.contains(userValue)) {
                        this.logMatch(comparisonAttribute, comparator, comparisonValue, value);
                        return value;
                    }
                    break;
                //IS NOT ONE OF
                case 1:
                    List<String> notInValues = new ArrayList<>(Arrays.asList(comparisonValue.split(",")));
                    notInValues.replaceAll(String::trim);
                    notInValues.removeAll(Arrays.asList(null, ""));
                    if(!notInValues.contains(userValue)) {
                        this.logMatch(comparisonAttribute, comparator, comparisonValue, value);
                        return value;
                    }
                    break;
                //CONTAINS
                case 2:
                    if(userValue.contains(comparisonValue)) {
                        this.logMatch(comparisonAttribute, comparator, comparisonValue, value);
                        return value;
                    }
                    break;
                //DOES NOT CONTAIN
                case 3:
                    if(!userValue.contains(comparisonValue)) {
                        this.logMatch(comparisonAttribute, comparator, comparisonValue, value);
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
                        Version userVersion = Version.valueOf(userValue);
                        boolean matched = false;
                        for (String semVer : inSemVerValues) {
                            matched = userVersion.equals(Version.valueOf(semVer)) || matched;
                        }

                        if ((matched && comparator == 4) || (!matched && comparator == 5)) {
                            this.logMatch(comparisonAttribute, comparator, comparisonValue, value);
                            return value;
                        }
                    } catch (ParseException e) {
                        this.logFormatError(comparisonAttribute, comparator, comparisonValue, e);
                        continue;
                    }
                    break;
                //LESS THAN, LESS THAN OR EQUALS TO, GREATER THAN, GREATER THAN OR EQUALS TO (SemVer)
                case 6:
                case 7:
                case 8:
                case 9:
                    try {
                        Version cmpUserVersion = Version.valueOf(userValue);
                        Version matchValue = Version.valueOf(comparisonValue.trim());
                        if( (comparator == 6 && cmpUserVersion.lessThan(matchValue)) ||
                                (comparator == 7 && cmpUserVersion.lessThanOrEqualTo(matchValue)) ||
                                (comparator == 8 && cmpUserVersion.greaterThan(matchValue)) ||
                                (comparator == 9 && cmpUserVersion.greaterThanOrEqualTo(matchValue))) {
                            this.logMatch(comparisonAttribute, comparator, comparisonValue, value);
                            return value;
                        }
                    } catch (ParseException e) {
                        this.logFormatError(comparisonAttribute, comparator, comparisonValue, e);
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
                            this.logMatch(comparisonAttribute, comparator, comparisonValue, value);
                            return value;
                        }
                    } catch (NumberFormatException e) {
                        this.logFormatError(comparisonAttribute, comparator, comparisonValue, e);
                        continue;
                    }
                    break;
            }
        }

        JsonArray percentageRules = json.getAsJsonArray("p");

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
                if(scaled < bucket)
                    return ruleObject.get("v");
            }
        }

        return json.get("v");
    }

    private void logMatch(String comparisonAttribute, int comparator, String comparisonValue, JsonElement value) {
        LOGGER.info("Evaluating rule: ["+comparisonAttribute+"] ["+COMPARATOR_TEXTS[comparator]+"] ["+comparisonValue+"] => match, returning: "+value+"");
    }

    private void logFormatError(String comparisonAttribute, int comparator, String comparisonValue, Exception exception) {
        LOGGER.warn("Evaluating rule: ["+comparisonAttribute+"] ["+COMPARATOR_TEXTS[comparator]+"] ["+comparisonValue+"] => SKIP rule. Validation error: "+exception+"");
    }
}
