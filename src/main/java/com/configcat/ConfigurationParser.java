package com.configcat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A json parser which can be used to deserialize configuration json strings.
 */
public class ConfigurationParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParser.class);
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();

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
        if(config == null || config.isEmpty())
            throw new IllegalArgumentException("config is null or empty");

        try {
            return gson.fromJson(config, classOfT);
        } catch (Exception e) {
            LOGGER.error("Parsing of the json ("+ config +") failed", e);
            throw new ParsingFailedException("Parsing failed.", config);
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
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        if(config == null || config.isEmpty())
            throw new IllegalArgumentException("config is null or empty");

        if(classOfT != String.class && classOfT != Integer.class && classOfT != Double.class && classOfT != Boolean.class)
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");

        try {
            JsonElement element = this.parser.parse(config).getAsJsonObject().get(key);
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
            throw new ParsingFailedException("Parsing failed.", config);
        }
    }
}
