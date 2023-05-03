package com.configcat;

import java.util.Map;
import java.util.TreeMap;

/**
 * An object containing attributes to properly identify a given user for variation evaluation.
 * Its only mandatory attribute is the {@code identifier}.
 */
public class User {
    private final String identifier;
    private final Map<String, String> attributes;

    private User(String identifier, String email, String country, Map<String, String> custom) {
        this.identifier = identifier == null ? "" : identifier;
        this.attributes = new TreeMap<>();
        this.attributes.put("Identifier", identifier);

        if (country != null && !country.isEmpty())
            this.attributes.put("Country", country);

        if (email != null && !email.isEmpty())
            this.attributes.put("Email", email);

        if (custom != null)
            this.attributes.putAll(custom);
    }

    public String getIdentifier() {
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

    public String getAttribute(String key) {
        if (key == null)
            throw new IllegalArgumentException("key is null or empty");

        return this.attributes.getOrDefault(key, null);
    }

    @Override
    public String toString() {
        return "User" + attributes + "";
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
