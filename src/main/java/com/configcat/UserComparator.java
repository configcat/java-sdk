package com.configcat;

/**
 * User Object attribute comparison operator used during the evaluation process.
 */
public enum UserComparator {

    /**
     * IS ONE OF (cleartext) - Checks whether the comparison attribute is equal to any of the comparison values.
     */
    IS_ONE_OF(0, "IS ONE OF"),
    /**
     * IS NOT ONE OF (cleartext) - Checks whether the comparison attribute is not equal to any of the comparison values.
     */
    IS_NOT_ONE_OF(1, "IS NOT ONE OF"),
    /**
     * CONTAINS ANY OF (cleartext) - Checks whether the comparison attribute contains any comparison values as a substring.
     */
    CONTAINS_ANY_OF(2, "CONTAINS ANY OF"),
    /**
     * NOT CONTAINS ANY OF (cleartext) - Checks whether the comparison attribute does not contain any comparison values as a substring.
     */
    NOT_CONTAINS_ANY_OF(3, "NOT CONTAINS ANY OF"),
    /**
     * IS ONE OF (semver) - Checks whether the comparison attribute interpreted as a semantic version is equal to any of the comparison values.
     */
    SEMVER_IS_ONE_OF(4, "IS ONE OF"),
    /**
     * IS NOT ONE OF (semver) - Checks whether the comparison attribute interpreted as a semantic version is not equal to any of the comparison values.
     */
    SEMVER_IS_NOT_ONE_OF(5, "IS NOT ONE OF"),
    /**
     * &lt; (semver) - Checks whether the comparison attribute interpreted as a semantic version is less than the comparison value.
     */
    SEMVER_LESS(6, "<"),
    /**
     * &lt;= (semver) - Checks whether the comparison attribute interpreted as a semantic version is less than or equal to the comparison value.
     */
    SEMVER_LESS_EQUALS(7, "<="),
    /**
     * &gt; (semver) - Checks whether the comparison attribute interpreted as a semantic version is greater than the comparison value.
     */
    SEMVER_GREATER(8, ">"),
    /**
     * &gt;= (semver) - Checks whether the comparison attribute interpreted as a semantic version is greater than or equal to the comparison value.
     */
    SEMVER_GREATER_EQUALS(9, ">="),
    /**
     * = (number) - Checks whether the comparison attribute interpreted as a decimal number is equal to the comparison value.
     */
    NUMBER_EQUALS(10, "="),
    /**
     * != (number) - Checks whether the comparison attribute interpreted as a decimal number is not equal to the comparison value.
     */
    NUMBER_NOT_EQUALS(11, "!="),
    /**
     * &lt; (number) - Checks whether the comparison attribute interpreted as a decimal number is less than the comparison value.
     */
    NUMBER_LESS(12, "<"),
    /**
     * &lt;= (number) - Checks whether the comparison attribute interpreted as a decimal number is less than or equal to the comparison value.
     */
    NUMBER_LESS_EQUALS(13, "<="),
    /**
     * &gt; (number) - Checks whether the comparison attribute interpreted as a decimal number is greater than the comparison value.
     */
    NUMBER_GREATER(14, ">"),
    /**
     * &gt;= (number) - Checks whether the comparison attribute interpreted as a decimal number is greater than or equal to the comparison value.
     */
    NUMBER_GREATER_EQUALS(15, ">="),
    /**
     * IS ONE OF (hashed) - Checks whether the comparison attribute is equal to any of the comparison values (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    SENSITIVE_IS_ONE_OF(16, "IS ONE OF"),
    /**
     * IS NOT ONE OF (hashed) - Checks whether the comparison attribute is not equal to any of the comparison values (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    SENSITIVE_IS_NOT_ONE_OF(17, "IS NOT ONE OF"),
    /**
     * BEFORE (UTC datetime) - Checks whether the comparison attribute interpreted as the seconds elapsed since <a href="https://en.wikipedia.org/wiki/Unix_time">Unix Epoch</a> is less than the comparison value.
     */
    DATE_BEFORE(18, "BEFORE"),
    /**
     * AFTER (UTC datetime) - Checks whether the comparison attribute interpreted as the seconds elapsed since <a href="https://en.wikipedia.org/wiki/Unix_time">Unix Epoch</a> is greater than the comparison value.
     */
    DATE_AFTER(19, "AFTER"),
    /**
     * EQUALS (hashed) - Checks whether the comparison attribute is equal to the comparison value (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    HASHED_EQUALS(20, "EQUALS"),
    /**
     * NOT EQUALS (hashed) - Checks whether the comparison attribute is not equal to the comparison value (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    HASHED_NOT_EQUALS(21, "NOT EQUALS"),
    /**
     * STARTS WITH ANY OF (hashed) - Checks whether the comparison attribute starts with any of the comparison values (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    HASHED_STARTS_WITH(22, "STARTS WITH ANY OF"),
    /**
     * NOT STARTS WITH ANY OF (hashed) - Checks whether the comparison attribute does not start with any of the comparison values (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    HASHED_NOT_STARTS_WITH(23, "NOT STARTS WITH ANY OF"),
    /**
     * ENDS WITH ANY OF (hashed) - Checks whether the comparison attribute ends with any of the comparison values (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    HASHED_ENDS_WITH(24, "ENDS WITH ANY OF"),
    /**
     * NOT ENDS WITH ANY OF (hashed) - Checks whether the comparison attribute does not end with any of the comparison values (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    HASHED_NOT_ENDS_WITH(25, "NOT ENDS WITH ANY OF"),
    /**
     * ARRAY CONTAINS ANY OF (hashed) - Checks whether the comparison attribute interpreted as a comma-separated list contains any of the comparison values (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    HASHED_ARRAY_CONTAINS(26, "ARRAY CONTAINS ANY OF"),
    /**
     * ARRAY NOT CONTAINS ANY OF (hashed) - Checks whether the comparison attribute interpreted as a comma-separated list does not contain any of the comparison values (where the comparison is performed using the salted SHA256 hashes of the values).
     */
    HASHED_ARRAY_NOT_CONTAINS(27, "ARRAY NOT CONTAINS ANY OF"),
    /**
     * EQUALS (cleartext) - Checks whether the comparison attribute is equal to the comparison value.
     */
    TEXT_EQUALS(28, "EQUALS"),
    /**
     * NOT EQUALS (cleartext) - Checks whether the comparison attribute is not equal to the comparison value.
     */
    TEXT_NOT_EQUALS(29, "NOT EQUALS"),
    /**
     * STARTS WITH ANY OF (cleartext) - Checks whether the comparison attribute starts with any of the comparison values.
     */
    TEXT_STARTS_WITH(30, "STARTS WITH ANY OF"),
    /**
     * NOT STARTS WITH ANY OF (cleartext) - Checks whether the comparison attribute does not start with any of the comparison values.
     */
    TEXT_NOT_STARTS_WITH(31, "NOT STARTS WITH ANY OF"),
    /**
     * ENDS WITH ANY OF (cleartext) - Checks whether the comparison attribute ends with any of the comparison values.
     */
    TEXT_ENDS_WITH(32, "ENDS WITH ANY OF"),
    /**
     * NOT ENDS WITH ANY OF (cleartext) - Checks whether the comparison attribute does not end with any of the comparison values.
     */
    TEXT_NOT_ENDS_WITH(33, "NOT ENDS WITH ANY OF"),
    /**
     * ARRAY CONTAINS ANY OF (cleartext) - Checks whether the comparison attribute interpreted as a comma-separated list contains any of the comparison values.
     */
    TEXT_ARRAY_CONTAINS(34, "ARRAY CONTAINS ANY OF"),
    /**
     * ARRAY NOT CONTAINS ANY OF (cleartext) - Checks whether the comparison attribute interpreted as a comma-separated list does not contain any of the comparison values.
     */
    TEXT_ARRAY_NOT_CONTAINS(35, "ARRAY NOT CONTAINS ANY OF");

    private final int id;
    private final String name;

    UserComparator(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static UserComparator fromId(int id) {
        for (UserComparator userComparator : UserComparator.values()) {
            if (userComparator.id == id) {
                return userComparator;
            }
        }
        return null;
    }

}
