package com.configcat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import java.util.Arrays;
import java.util.List;

class RolloutEvaluator {
    public JsonElement evaluate(JsonObject json, String key, User user) {
        if(user == null)
            return json.get("Value");

        JsonArray rolloutRules = json.getAsJsonArray("RolloutRules");

        for (JsonElement rule: rolloutRules) {
            JsonObject ruleObject = rule.getAsJsonObject();

            String comparisonAttribute = ruleObject.get("ComparisonAttribute").getAsString();
            String comparisonValue = ruleObject.get("ComparisonValue").getAsString();
            int comparator = ruleObject.get("Comparator").getAsInt();
            JsonElement value = ruleObject.get("Value");
            String userValue = user.getAttribute(comparisonAttribute);

            if(comparisonValue == null || comparisonValue.isEmpty() ||
                    userValue == null || userValue.isEmpty())
                continue;

            switch (comparator) {
                case 0:
                    List<String> inValues = Arrays.asList(comparisonValue.split(","));
                    inValues.replaceAll(String::trim);
                    if(inValues.contains(userValue))
                        return value;
                    break;
                case 1:
                    List<String> notInValues = Arrays.asList(comparisonValue.split(","));
                    notInValues.replaceAll(String::trim);
                    if(!notInValues.contains(userValue))
                        return value;
                    break;
                case 2:
                    if(userValue.contains(comparisonValue))
                        return value;
                    break;
                case 3:
                    if(!userValue.contains(comparisonValue))
                        return value;
                    break;
            }
        }

        JsonArray percentageRules = json.getAsJsonArray("RolloutPercentageItems");

        if(percentageRules.size() > 0){
            String hashCandidate = key + user.getIdentifier();
            long scale = 100;
            String hexHash = DigestUtils.sha1Hex(hashCandidate).substring(0, 15);
            long longHash = Long.parseLong(hexHash, 16);
            long scaled = longHash % scale;

            int bucket = 0;
            for (JsonElement rule: percentageRules) {
                JsonObject ruleObject = rule.getAsJsonObject();

                bucket += ruleObject.get("Percentage").getAsInt();
                if(scaled < bucket)
                    return ruleObject.get("Value");
            }
        }

        return json.get("Value");
    }
}
