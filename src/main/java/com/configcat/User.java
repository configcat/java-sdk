package com.configcat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * An object containing attributes to properly identify a given user for variation evaluation.
 * Its only mandatory attribute is the {@code identifier}.
 * <p>
 * Please note that the {@code User} class is not designed to be used as a DTO (data transfer object).
 * (Since the type of the {@code attributes} property is polymorphic, it's not guaranteed that deserializing a serialized instance produces an instance with an identical or even valid data content.)
 */
public class User {
    private static final String IDENTIFIER_KEY = "Identifier";
    private static final String EMAIL = "Email";
    private static final String COUNTRY = "Country";
    private final String identifier;
    private final Map<String, Object> attributes;

    private User(String identifier, String email, String country, Map<String, Object> custom) {
        this.identifier = identifier == null ? "" : identifier;
        this.attributes = new TreeMap<>();
        this.attributes.put(IDENTIFIER_KEY, identifier);

        if (country != null && !country.isEmpty()) {
            this.attributes.put(COUNTRY, country);
        }

        if (email != null && !email.isEmpty()) {
            this.attributes.put(EMAIL, email);
        }

        if (custom != null) {
            for (Map.Entry<String, Object> entry :custom.entrySet()) {
                if(!entry.getKey().equals(IDENTIFIER_KEY) && !entry.getKey().equals(COUNTRY) && !entry.getKey().equals(EMAIL)){
                    this.attributes.put(entry.getKey(), entry.getValue());
                }
            }
        }
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

    Object getAttribute(String key) {
        if (key == null)
            throw new IllegalArgumentException("key is null or empty");

        return this.attributes.getOrDefault(key, null);
    }

    @Override
    public String toString() {

        LinkedHashMap<String, Object> tmp = new LinkedHashMap<>();
        if (attributes.containsKey(IDENTIFIER_KEY)) {
            tmp.put(IDENTIFIER_KEY, attributes.get(IDENTIFIER_KEY));
        }
        if (attributes.containsKey(EMAIL)) {
            tmp.put(EMAIL, attributes.get(EMAIL));
        }
        if (attributes.containsKey(COUNTRY)) {
            tmp.put(COUNTRY, attributes.get(COUNTRY));
        }
        for (Map.Entry<String, Object> entry :attributes.entrySet()) {
            if(!entry.getKey().equals(IDENTIFIER_KEY) && !entry.getKey().equals(COUNTRY) && !entry.getKey().equals(EMAIL)){
                tmp.put(entry.getKey(), entry.getValue());
            }
        }
        return Utils.gson.toJson(tmp);
    }

    /**
     * A builder that helps construct a {@link User} instance.
     */
    public static class Builder {
        private String email;
        private String country;
        private Map<String, Object> custom;

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
         * <p>
         * Custom attributes of the user for advanced targeting rule definitions (e.g. user role, subscription type, etc.)
         * <p>
         * The set of allowed attribute values depends on the comparison type of the condition which references the User Object attribute.<br>
         * {@link String} values are supported by all comparison types (in some cases they need to be provided in a specific format though).<br>
         * Some of the comparison types work with other types of values, as described below.
         * <p>
         * Text-based comparisons (EQUALS, IS ONE OF, etc.)<br>
         *   <ul>
         *       <li> accept {@link String} values,
         *       <li> all other values are automatically converted to string (a warning will be logged but evaluation will continue as normal).
         *   </ul>
         * <p>
         * SemVer-based comparisons (IS ONE OF, &lt;, &gt;=, etc.)<br>
         *   <ul>
         *       <li> accept {@link String} values containing a properly formatted, valid semver value,
         *       <li> all other values are considered invalid (a warning will be logged and the currently evaluated targeting rule will be skipped).
         *   </ul>
         * <p>
         * Number-based comparisons (=, &lt;, &gt;=, etc.)<br>
         *  <ul>
         *      <li> accept {@link Double} values (except for {@code Double.NaN}) and all other numeric values which can safely be converted to {@link Double}
         *      <li> accept {@link String} values containing a properly formatted, valid {@link Double} value
         *      <li> all other values are considered invalid (a warning will be logged and the currently evaluated targeting rule will be skipped).
         *    </ul>
         * <p>
         * Date time-based comparisons (BEFORE / AFTER)<br>
         *     <ul>
         *         <li> accept {@link java.util.Date} values, which are automatically converted to a second-based Unix timestamp
         *         <li> accept {@link java.time.Instant} values, which are automatically converted to a second-based Unix timestamp
         *         <li> accept {@link Double} values (except for {@code Double.NaN}) representing a second-based Unix timestamp and all other numeric values which can safely be converted to {@link Double}
         *         <li> accept {@link String} values containing a properly formatted, valid {@link Double} value
         *         <li> all other values are considered invalid (a warning will be logged and the currently evaluated targeting rule will be skipped).
         *     </ul>
         * <p>
         * String array-based comparisons (ARRAY CONTAINS ANY OF / ARRAY NOT CONTAINS ANY OF)<br>
         *     <ul>
         *         <li> accept arrays of {@link String}
         *         <li> accept {@link java.util.List} of {@link String}
         *         <li> accept {@link String} values containing a valid JSON string which can be deserialized to an array of {@link String}
         *         <li> all other values are considered invalid (a warning will be logged and the currently evaluated targeting rule will be skipped).
         *      </ul>
         * <p>
         * In case a non-string attribute value needs to be converted to {@link String} during evaluation, it will always be done using the same format which is accepted by the comparisons.
         *
         * @param custom the custom attributes.
         * @return the builder.
         */
        public Builder custom(Map<String, Object> custom) {
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
