package com.configcat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class DateTimeUtils {

    private DateTimeUtils() { /* prevent from instantiation*/ }

    /**
     * HTTP Date header formatter. Date: day-name, day month year hour:minute:second GMT
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Date">mdn docs</a>
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

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

