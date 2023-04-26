package com.configcat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class CacheUtils {

    private CacheUtils() { /* prevent from instantiation*/ }

    public static final class DateTimeUtils {

        /**
         * HTTP Date header formatter. Date: day-name, day month year hour:minute:second GMT

         *  @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Date">mdn docs</a>
         */
        public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

        public static boolean isValidDate(String date) {
            try {
                DATE_TIME_FORMATTER.parse(date);
            } catch (DateTimeParseException e) {
                return false;
            }
            return true;
        }

        public static long parseToMillis(String dateTime) {
            return Instant.EPOCH.until(Instant.from(DateTimeUtils.DATE_TIME_FORMATTER.parse(dateTime)), ChronoUnit.MILLIS);

        }

        public static String format(long timeInMilliseconds) {
            return DateTimeUtils.DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timeInMilliseconds));
        }
    }

    public static String serialize(final Entry entry) {
        return entry.getFetchTimeRaw() + "\n" + entry.getETag() + "\n" + entry.getConfigJson();
    }

    public static Entry deserialize(String cacheValue) throws Exception {
        if (cacheValue == null || cacheValue.isEmpty()) {
            return Entry.EMPTY;
        }

        int fetchTimeIndex = cacheValue.indexOf("\n");
        int eTagIndex = cacheValue.indexOf("\n", fetchTimeIndex + 1);
        if (fetchTimeIndex < 0 || eTagIndex < 0) {
            throw new Exception("Number of values is fewer than expected.");
        }
        String fetchTimeRaw = cacheValue.substring(0, fetchTimeIndex);
        if (!DateTimeUtils.isValidDate(fetchTimeRaw)) {
            throw new Exception("Invalid fetch time: " + fetchTimeRaw);
        }

        String eTag = cacheValue.substring(fetchTimeIndex + 1, eTagIndex);
        if (eTag.isEmpty()) {
            throw new Exception("Empty eTag value.");
        }
        String configJson = cacheValue.substring(eTagIndex + 1);
        if (configJson.isEmpty()) {
            throw new Exception("Empty config jsom value.");
        }
        try {
            Config config = Utils.gson.fromJson(configJson, Config.class);
            return new Entry(config, eTag, configJson, fetchTimeRaw);
        } catch (Exception e) {
            throw new Exception("Invalid config JSON content: " + configJson);
        }
    }
}
