package com.configcat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

/**
 * A json parser which can be used to deserialize configuration json strings.
 */
public class ConfigurationParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParser.class);
    private final JsonParser parser = new JsonParser();
    private final RolloutEvaluator rolloutEvaluator = new RolloutEvaluator();

    /**
     * Parses a json element identified by the {@code key} from the given json string into a primitive type (Boolean, Double, Integer or String).
     * @param classOfT the class of T.
     * @param config the json config.
     * @param key the key of the value.
     * @param <T> the type of the desired object.
     * @return the parsed value.
     * @throws ParsingFailedException when the parsing failed.
     * @throws IllegalArgumentException when the key or the config is empty or null, or when the {@code <T>} type is not supported.
     */
    public <T> T parseValue(Class<T> classOfT, String config, String key) throws ParsingFailedException, IllegalArgumentException {
        return this.parseValue(classOfT, config, key, null);
    }

    /**
     * Parses a json element identified by the {@code key} from the given json string into a primitive type (Boolean, Double, Integer or String).
     * @param classOfT the class of T.
     * @param config the json config.
     * @param key the key of the value.
     * @param user the user object to identify the caller.
     * @param <T> the type of the desired object.
     * @return the parsed value.
     * @throws ParsingFailedException when the parsing failed.
     * @throws IllegalArgumentException when the key or the config is empty or null, or when the {@code <T>} type is not supported.
     */
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

    /**
     * Gets all setting keys from the config json.
     *
     * @param config the json config.
     * @return a collection of the setting keys.
     */
    public Collection<String> getAllKeys(String config) throws ParsingFailedException {
        try {
            JsonObject root = this.parser.parse(config).getAsJsonObject();
            return root.keySet();

        } catch (Exception e) {
            LOGGER.error("Parsing of json ("+ config +") failed.", e);
            throw new ParsingFailedException("Parsing failed.", config, e);
        }
    }

    private Object parseValueInternal(Class<?> classOfT, String config, String key, User user) throws ParsingFailedException, IllegalArgumentException {
        try {
            JsonObject root = this.parser.parse(config).getAsJsonObject();
            JsonElement element = this.rolloutEvaluator.evaluate(root.getAsJsonObject(key), key, user);
            if (classOfT == String.class)
                return element.getAsString();
            else if (classOfT == Integer.class || classOfT == int.class)
                return element.getAsInt();
            else if (classOfT == Double.class || classOfT == double.class)
                return element.getAsDouble();
            else
                return element.getAsBoolean();
        } catch (Exception e) {
            LOGGER.error("Parsing of json ("+ config +") failed.", e);
            throw new ParsingFailedException("Parsing failed.", config, e);
        }
    }
}
