package com.configcat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A json parser which can be used to deserialize configuration json strings.
 */
public class ConfigurationParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParser.class);
    private final JsonParser parser = new JsonParser();
    private final RolloutEvaluator rolloutEvaluator = new RolloutEvaluator();

    /**
     * Parses a json string into the given {@code <T>} type.
     *
     * @param classOfT the class of T.
     * @param config the json config.
     * @param <T> the type of the desired object.
     * @return the parsed object.
     * @throws ParsingFailedException when the parsing failed.
     * @throws IllegalArgumentException when the config is empty or null.
     */
    public <T> T parse(Class<T> classOfT, String config) throws ParsingFailedException, IllegalArgumentException {
        return this.parse(classOfT, config, null);
    }

    /**
     * Parses a json string into the given {@code <T>} type.
     *
     * @param classOfT the class of T.
     * @param config the json config.
     * @param user the user object to identify the caller.
     * @param <T> the type of the desired object.
     * @return the parsed object.
     * @throws ParsingFailedException when the parsing failed.
     * @throws IllegalArgumentException when the config is empty or null.
     */
    public <T> T parse(Class<T> classOfT, String config, User user) throws ParsingFailedException, IllegalArgumentException {
        if(config == null || config.isEmpty())
            throw new IllegalArgumentException("config is null or empty");

        try {
            Object instance = classOfT.newInstance();
            Field[] fields = classOfT.getDeclaredFields();
            for (Field field : fields) {
                if(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                    continue;

                Class<?> fieldType = field.getType();
                String fieldName = field.getName();
                try {
                    field.setAccessible(true);
                    field.set(instance, this.parseValueInternal(fieldType, config, fieldName, user));
                } catch (Exception e) {
                    LOGGER.error("Failed to set field ("+ fieldName +")", e);
                    throw e;
                }
            }

            return classOfT.cast(instance);

        } catch (Exception e) {
            LOGGER.error("Parsing of the json ("+ config +") failed", e);
            throw new ParsingFailedException("Parsing failed.", config, e);
        }
    }

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
    public <T> T parseValue(Class<T> classOfT, String config, String key, User user) throws ParsingFailedException, IllegalArgumentException {
        return classOfT.cast(this.parseValueInternal(classOfT, config, key, user));
    }

    private Object parseValueInternal(Class<?> classOfT, String config, String key, User user) throws ParsingFailedException, IllegalArgumentException {
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        if(config == null || config.isEmpty())
            throw new IllegalArgumentException("config is null or empty");

        if(classOfT != String.class && classOfT != Integer.class && classOfT != Double.class && classOfT != Boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");

        try {
            JsonObject root = this.parser.parse(config).getAsJsonObject();
            JsonElement element = this.rolloutEvaluator.evaluate(root.getAsJsonObject(key), key, user);
            if (classOfT == String.class)
                return classOfT.cast(element.getAsString());
            else if (classOfT == Integer.class)
                return classOfT.cast(element.getAsInt());
            else if (classOfT == Double.class)
                return classOfT.cast(element.getAsDouble());
            else
                return classOfT.cast(element.getAsBoolean());
        } catch (Exception e) {
            LOGGER.error("Parsing of the json ("+ config +") failed", e);
            throw new ParsingFailedException("Parsing failed.", config, e);
        }
    }
}
