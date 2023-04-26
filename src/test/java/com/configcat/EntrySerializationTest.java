package com.configcat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntrySerializationTest {

    private static final String TEST_JSON = "{ f: { fakeKey: { v: %s, t: %s, p: [] ,r: [] } } }";

    private static final String SERIALIZED_DATA = "%s\n%s\n%s";

    @Test
    public void serialize() {
        String json = String.format(TEST_JSON, "test", "1");
        Config config = Utils.gson.fromJson(json, Config.class);
        String fetchTimeRaw = DateTimeUtils.format(System.currentTimeMillis());
        Entry entry = new Entry(config, "fakeTag", json, fetchTimeRaw);

        String serializedString = entry.serialize();

        assertEquals(String.format(SERIALIZED_DATA, fetchTimeRaw, "fakeTag", json), serializedString);
    }

    @Test
    public void deserialize() throws Exception {
        String json = String.format(TEST_JSON, "test", "1");
        long currentTimeMillis = System.currentTimeMillis();
        String fetchTimeRaw = DateTimeUtils.format(currentTimeMillis);

        Entry entry = Entry.fromString(String.format(SERIALIZED_DATA, fetchTimeRaw, "fakeTag", json));

        assertNotNull(entry);
        assertEquals(entry.getFetchTimeRaw(), fetchTimeRaw);
        assertEquals(entry.getETag(), "fakeTag");
        assertEquals(entry.getConfigJson(), json);
        assertEquals(entry.getConfig().getEntries().size(), 1);
        assertEquals(entry.getFetchTime(), Math.ceil(currentTimeMillis / 1000) * 1000);
    }

    @Test
    public void deserializeMissingValue() throws Exception {
        Entry deserializeNull = Entry.fromString(null);
        assertTrue(deserializeNull.isEmpty());
        Entry deserializeEmpty = Entry.fromString("");
        assertTrue(deserializeEmpty.isEmpty());
    }

    @Test
    public void deserializeWrongFormat() throws Exception {
        Exception assertThrows = assertThrows(Exception.class, () -> Entry.fromString("value with no new line"));
        assertEquals("Number of values is fewer than expected.", assertThrows.getMessage());

        assertThrows = assertThrows(Exception.class, () -> Entry.fromString("value with one \n new line"));
        assertEquals("Number of values is fewer than expected.", assertThrows.getMessage());
    }

    @Test
    public void deserializeInvalidDate() throws Exception {
        Exception assertThrows = assertThrows(Exception.class, () -> Entry.fromString(String.format(SERIALIZED_DATA, "invalid", "fakeTag", "json")));

        assertEquals("Invalid fetch time: invalid", assertThrows.getMessage());
    }

    @Test
    public void deserializeInvalidETag() {
        long currentTimeMillis = System.currentTimeMillis();
        String fetchTimeRaw = DateTimeUtils.format(currentTimeMillis);
        Exception assertThrows = assertThrows(Exception.class, () -> Entry.fromString(String.format(SERIALIZED_DATA, fetchTimeRaw, "", "json")));

        assertEquals("Empty eTag value.", assertThrows.getMessage());
    }

    @Test
    public void deserializeInvalidJson() {
        long currentTimeMillis = System.currentTimeMillis();
        String fetchTimeRaw = DateTimeUtils.format(currentTimeMillis);
        Exception assertThrows = assertThrows(Exception.class, () -> Entry.fromString(String.format(SERIALIZED_DATA, fetchTimeRaw, "fakeTag", "")));

        assertEquals("Empty config jsom value.", assertThrows.getMessage());

        assertThrows = assertThrows(Exception.class, () -> Entry.fromString(String.format(SERIALIZED_DATA, fetchTimeRaw, "fakeTag", "wrongjson")));

        assertEquals("Invalid config JSON content: wrongjson", assertThrows.getMessage());

    }

}
