package com.configcat;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationParserTest {
    private final static ConfigurationParser parser = new ConfigurationParser(LoggerFactory.getLogger(ConfigurationParserTest.class));

    @Test
    public void parseValueThrowsArgumentInvalid() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseValue(Object.class, null, "key"));
        assertThrows(IllegalArgumentException.class, () -> parser.parseValue(Object.class, "", "key"));
        assertThrows(IllegalArgumentException.class, () -> parser.parseValue(Object.class, "config", null));
        assertThrows(IllegalArgumentException.class, () -> parser.parseValue(Object.class, "config", ""));
        assertThrows(IllegalArgumentException.class, () -> parser.parseValue(Object.class, "config", "key"));
    }

    @Test
    public void parseValueThrowsInvalidJson() {
        String badJson = "{ test: test] }";
        ParsingFailedException exp = assertThrows(ParsingFailedException.class, () -> parser.parseValue(String.class, badJson, "test"));
        assertEquals(badJson, exp.getJson());
        assertTrue(exp.getInnerException() instanceof JsonSyntaxException);
    }
}
