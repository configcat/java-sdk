package com.configcat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class UserAttributeConverter {

    public static String userAttributeToString(Object userAttribute) {
        if (userAttribute == null) {
            return null;
        }
        if (userAttribute instanceof String) {
            return (String) userAttribute;
        }
        if (userAttribute instanceof String[]) {
            return Utils.gson.toJson(userAttribute);
        }
        if (userAttribute instanceof List) {
            return Utils.gson.toJson(userAttribute);
        }
        if (userAttribute instanceof Date) {
            Date userAttributeDate = (Date) userAttribute;
            return doubleToString(DateTimeUtils.getUnixSeconds(userAttributeDate));
        }
        if (userAttribute instanceof Instant) {
            Instant userAttributeInstant = (Instant) userAttribute;
            return doubleToString(DateTimeUtils.getUnixSeconds(userAttributeInstant));
        }
        if (userAttribute instanceof Double) {
            return doubleToString((Double) userAttribute);
        }
        return userAttribute.toString();
    }

    private static String doubleToString(Double doubleToString) {

        // Handle Double.NaN, Double.POSITIVE_INFINITY and Double.NEGATIVE_INFINITY
        if (doubleToString.isNaN() || doubleToString.isInfinite()) {
            return doubleToString.toString();
        }

        // To get similar result between different SDKs the Double value format is modified.
        // Between 1e-7 and 1e21 we don't use scientific-notation. Over these limits scientific-notation used but the
        // ExponentSeparator replaced with "e" and "e+".
        // "." used as decimal separator in all cases.
        double abs = Math.abs(doubleToString);
        DecimalFormat fmt = 1e-6 <= abs && abs < 1e21
                ? new DecimalFormat("#.#################")
                : new DecimalFormat("#.#################E0");
        DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.UK);
        if (doubleToString > 1 || doubleToString < -1) {
            SYMBOLS.setExponentSeparator("e+");
        } else {
            SYMBOLS.setExponentSeparator("e");
        }
        fmt.setDecimalFormatSymbols(SYMBOLS);
        return fmt.format(doubleToString);
    }

    public static Double userAttributeToDouble(Object userAttribute) {
        if (userAttribute == null) {
            return null;
        }
        if (userAttribute instanceof Double) {
            return (Double) userAttribute;
        }
        if (userAttribute instanceof String) {
            return Double.parseDouble(((String) userAttribute).trim().replace(",", "."));
        }
        if (userAttribute instanceof Integer) {
            return ((Integer) userAttribute).doubleValue();
        }
        if (userAttribute instanceof Float) {
            return ((Float) userAttribute).doubleValue();
        }
        if (userAttribute instanceof Long) {
            return ((Long) userAttribute).doubleValue();
        }
        if (userAttribute instanceof Byte) {
            return ((Byte) userAttribute).doubleValue();
        }
        if (userAttribute instanceof Short) {
            return ((Short) userAttribute).doubleValue();
        }

        throw new NumberFormatException();
    }

}
