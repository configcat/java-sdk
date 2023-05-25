package com.configcat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntrySerializationTest {

    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, t: %s, p: [] ,r: [] } } }";

    private static final String SERIALIZED_DATA = "%s\n%s\n%s";

    @Test
    void serialize() {
        String json = String.format(TEST_JSON, "test", "1");
        Config config = Utils.gson.fromJson(json, Config.class);
        long fetchTime = System.currentTimeMillis();
        Entry entry = new Entry(config, "fakeTag", json, fetchTime);

        String serializedString = entry.serialize();

        assertEquals(String.format(SERIALIZED_DATA, fetchTime / 1000, "fakeTag", json), serializedString);
    }

    @Test
    void deserialize() throws Exception {
        String json = String.format(TEST_JSON, "test", "1");
        long currentTimeMillis = System.currentTimeMillis();

        Entry entry = Entry.fromString(String.format(SERIALIZED_DATA, currentTimeMillis / 1000, "fakeTag", json));

        assertNotNull(entry);
        assertEquals("fakeTag", entry.getETag());
        assertEquals(json, entry.getConfigJson());
        assertEquals(1, entry.getConfig().getEntries().size());
        assertEquals(Math.ceil(currentTimeMillis / 1000) * 1000, entry.getFetchTime());
    }

    @Test
    void deserializeMissingValue() throws Exception {
        Entry deserializeNull = Entry.fromString(null);
        assertTrue(deserializeNull.isEmpty());
        Entry deserializeEmpty = Entry.fromString("");
        assertTrue(deserializeEmpty.isEmpty());
    }

    @Test
    void deserializeWrongFormat() {
        Exception assertThrows = assertThrows(Exception.class, () -> Entry.fromString("value with no new line"));
        assertEquals("Number of values is fewer than expected.", assertThrows.getMessage());

        assertThrows = assertThrows(Exception.class, () -> Entry.fromString("value with one \n new line"));
        assertEquals("Number of values is fewer than expected.", assertThrows.getMessage());
    }

    @Test
    void deserializeInvalidDate() {
        Exception assertThrows = assertThrows(Exception.class, () -> Entry.fromString(String.format(SERIALIZED_DATA, "invalid", "fakeTag", "json")));

        assertEquals("Invalid fetch time: invalid", assertThrows.getMessage());
    }

    @Test
    void deserializeInvalidETag() {
        long currentTimeMillis = System.currentTimeMillis();
        Exception assertThrows = assertThrows(Exception.class, () -> Entry.fromString(String.format(SERIALIZED_DATA, currentTimeMillis / 1000, "", "json")));

        assertEquals("Empty eTag value.", assertThrows.getMessage());
    }

    @Test
    void deserializeInvalidJson() {
        long currentTimeMillis = System.currentTimeMillis();
        Exception assertThrows = assertThrows(Exception.class, () -> Entry.fromString(String.format(SERIALIZED_DATA, currentTimeMillis / 1000, "fakeTag", "")));

        assertEquals("Empty config jsom value.", assertThrows.getMessage());

        assertThrows = assertThrows(Exception.class, () -> Entry.fromString(String.format(SERIALIZED_DATA, currentTimeMillis / 1000, "fakeTag", "wrongjson")));

        assertEquals("Invalid config JSON content: wrongjson", assertThrows.getMessage());

    }

}
