package com.configcat;

import java.util.Map;
import java.util.TreeMap;

/**
 * An object containing attributes to properly identify a given user for variation evaluation.
 * Its only mandatory attribute is the {@code identifier}.
 */
public class User {
    private String identifier;
    private Map<String, String> attributes;

    private User(String identifier, String email, String country,  Map<String, String> custom) {
        if(identifier == null || identifier.isEmpty())
            throw new IllegalArgumentException("identifier is null or empty");

        this.identifier = identifier;
        this.attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.attributes.put("identifier", identifier);

        if(country != null && !country.isEmpty())
            this.attributes.put("country", country);

        if(email != null && !email.isEmpty())
            this.attributes.put("email", email);

        if(custom != null)
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
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        return this.attributes.getOrDefault(key, null);
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
