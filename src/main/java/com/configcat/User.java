package com.configcat;

import java.text.DecimalFormat;
import java.util.*;

/**
 * An object containing attributes to properly identify a given user for variation evaluation.
 * Its only mandatory attribute is the {@code identifier}.
 */
public class User {
    private static final String IDENTIFIER = "Identifier";
    private static final String EMAIL = "Email";
    private static final String COUNTRY = "Country";
    private final String identifier;
    private final Map<String, String> attributes;

    private User(String identifier, String email, String country, Map<String, String> custom) {
        this.identifier = identifier == null ? "" : identifier;
        this.attributes = new TreeMap<>();
        this.attributes.put(IDENTIFIER, identifier);

        if (country != null && !country.isEmpty())
            this.attributes.put(COUNTRY, country);

        if (email != null && !email.isEmpty())
            this.attributes.put(EMAIL, email);

        if (custom != null)
            this.attributes.putAll(custom);
    }

    String getIdentifier() {
        return this.identifier;
    }

    /**
     * Creates a new builder instance.
     *
     * @return the new builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    String getAttribute(String key) {
        if (key == null)
            throw new IllegalArgumentException("key is null or empty");

        return this.attributes.getOrDefault(key, null);
    }

    @Override
    public String toString() {

        LinkedHashMap<String, String> tmp = new LinkedHashMap<>();
        if (attributes.containsKey(IDENTIFIER)) {
            tmp.put(IDENTIFIER, attributes.get(IDENTIFIER));
        }
        if (attributes.containsKey(EMAIL)) {
            tmp.put(EMAIL, attributes.get(EMAIL));
        }
        if (attributes.containsKey(COUNTRY)) {
            tmp.put(COUNTRY, attributes.get(COUNTRY));
        }
        tmp.putAll(attributes);
        StringBuilder userStringBuilder = new StringBuilder();
        userStringBuilder.append('{');
        Iterator it = tmp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            userStringBuilder.append('"').append(me.getKey()).append("\":\"").append(me.getValue()).append('"');
            if (it.hasNext()) {
                userStringBuilder.append(',');
            }
        }
        userStringBuilder.append('}');
        return userStringBuilder.toString();
    }

    public static String attributeValueFrom(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Invalid 'date' parameter.");
        }
        double unixSeconds = DateTimeUtils.getUnixSeconds(date);
        DecimalFormat decimalFormat = Utils.getDecimalFormat();

        return decimalFormat.format(unixSeconds);
    }

    public static String attributeValueFrom(double number) {
        return String.valueOf(number);
    }

    public static String attributeValueFrom(int number) {
        return String.valueOf(number);
    }

    public static String attributeValueFrom(String[] items) {
        if (items == null) {
            throw new IllegalArgumentException("Invalid 'items' parameter.");
        }
        return Utils.gson.toJson(items);
    }

    /**
     * A builder that helps construct a {@link User} instance.
     */
    public static class Builder {
        private String email;
        private String country;
        private Map<String, String> custom;

        /**
         * Optional. Sets the email of the user.
         *
         * @param email the email address.
         * @return the builder.
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Optional. Sets the country of the user.
         *
         * @param country the country.
         * @return the builder.
         */
        public Builder country(String country) {
            this.country = country;
            return this;
        }

        /**
         * Optional. Sets the custom attributes of a user
         *
         * @param custom the custom attributes.
         * @return the builder.
         */
        public Builder custom(Map<String, String> custom) {
            this.custom = custom;
            return this;
        }

        /**
         * Builds the configured {@link User} instance.
         *
         * @param identifier the user identifier.
         * @return the configured {@link User} instance.
         */
        public User build(String identifier) {
            return new User(identifier, this.email, this.country, this.custom);
        }
    }
}
