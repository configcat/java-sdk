package com.configcat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

class ConfigurationParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParser.class);
    private final JsonParser parser = new JsonParser();
    private final RolloutEvaluator rolloutEvaluator = new RolloutEvaluator();

    public <T> T parseValue(Class<T> classOfT, String config, String key) throws ParsingFailedException, IllegalArgumentException {
        return this.parseValue(classOfT, config, key, null);
    }

    public <T> T parseValue(Class<T> classOfT, String config, String key, User user) throws ParsingFailedException,
            IllegalArgumentException {
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        if(config == null || config.isEmpty())
            throw new IllegalArgumentException("config is null or empty");

        if(classOfT != String.class &&
                classOfT != Integer.class &&
                classOfT != int.class &&
                classOfT != Double.class &&
                classOfT != double.class &&
                classOfT != Boolean.class &&
                classOfT != boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");

        return (T)parseValueInternal(classOfT, config, key, user);
    }

    public String parseVariationId(String config, String key, User user) throws ParsingFailedException {
        try {
            LOGGER.info("Evaluating getVariationId("+key+").");
            JsonObject root = this.parser.parse(config).getAsJsonObject();

            JsonObject node = root.getAsJsonObject(key);
            if(node == null) {
                throw new ParsingFailedException("Variation ID not found for key "+key+". Here are the available keys: " + String.join(", ", root.keySet()), config);
            }

            return this.rolloutEvaluator.evaluate(node, key, user).getValue().getAsString();
        } catch (ParsingFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingFailedException("JSON Parsing failed.", config, e);
        }
    }

    public <T> Map.Entry<String, T> parseKeyValue(Class<T> classOfT, String config, String variationId) throws ParsingFailedException {
        try {
            Set<Map.Entry<String, JsonElement>> root = this.parser.parse(config).getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> node: root) {
                String settingKey = node.getKey();
                JsonObject setting = node.getValue().getAsJsonObject();
                if(variationId.equals(setting.get(Setting.VariationId).getAsString())) {
                    return new AbstractMap.SimpleEntry<>(settingKey, (T)this.parseObject(classOfT, setting.get(Setting.Value)));
                }

                JsonArray rolloutRules = setting.get(Setting.RolloutRules).getAsJsonArray();
                for (JsonElement rolloutElement : rolloutRules) {
                    JsonObject rolloutRule = rolloutElement.getAsJsonObject();
                    if(variationId.equals(rolloutRule.get(RolloutRules.VariationId).getAsString())) {
                        return new AbstractMap.SimpleEntry<>(settingKey, (T)this.parseObject(classOfT, rolloutRule.get(RolloutRules.Value)));
                    }
                }

                JsonArray persentageRules = setting.get(Setting.RolloutPercentageItems).getAsJsonArray();
                for (JsonElement percentageElement : persentageRules) {
                    JsonObject percentageRule = percentageElement.getAsJsonObject();
                    if(variationId.equals(percentageRule.get(RolloutPercentageItems.VariationId).getAsString())) {
                        return new AbstractMap.SimpleEntry<>(settingKey, (T)this.parseObject(classOfT, percentageRule.get(RolloutPercentageItems.Value)));
                    }
                }
            }

            return null;
        } catch (Exception e) {
            throw new ParsingFailedException("JSON Parsing failed.", config, e);
        }
    }

    public Collection<String> getAllKeys(String config) throws ParsingFailedException {
        try {
            JsonObject root = this.parser.parse(config).getAsJsonObject();
            return root.keySet();

        } catch (Exception e) {
            throw new ParsingFailedException("JSON Parsing failed.", config, e);
        }
    }

    private Object parseValueInternal(Class<?> classOfT, String config, String key, User user) throws ParsingFailedException, IllegalArgumentException {
        try {
            JsonObject root = this.parser.parse(config).getAsJsonObject();

            JsonObject node = root.getAsJsonObject(key);
            if(node == null) {
                throw new ParsingFailedException("Value not found for key "+key+". Here are the available keys: " + String.join(", ", root.keySet()), config);
            }

            return this.parseObject(classOfT, this.rolloutEvaluator.evaluate(node, key, user).getKey());
        } catch (ParsingFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingFailedException("JSON Parsing failed.", config, e);
        }
    }

    private Object parseObject(Class<?> classOfT, JsonElement element) {
        if (classOfT == String.class)
            return element.getAsString();
        else if (classOfT == Integer.class || classOfT == int.class)
            return element.getAsInt();
        else if (classOfT == Double.class || classOfT == double.class)
            return element.getAsDouble();
        else
            return element.getAsBoolean();
    }
}
