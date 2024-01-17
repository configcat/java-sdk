package com.configcat;

import java.util.Date;
import java.util.List;

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
            return String.valueOf(DateTimeUtils.getUnixSeconds(userAttributeDate));
        }
        return userAttribute.toString();
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
