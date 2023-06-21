package com.configcat;

public class DateTimeUtils {

    private DateTimeUtils() { /* prevent from instantiation*/ }

    public static boolean isValidDate(String date) {
        try {
            Long.parseLong(date);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
