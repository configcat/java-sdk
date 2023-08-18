package com.configcat;

/**
 * Describes the Rollout Evaluator Comparators.
 */
public enum Comparator {
    CONTAINS_ANY_OF(2, "CONTAINS ANY OF"),
    NOT_CONTAINS_ANY_OF(3, "NOT CONTAINS ANY OF"),
    SEMVER_IS_ONE_OF(4, "IS ONE OF (semver)"),
    SEMVER_IS_NOT_ONE_OF(5, "IS NOT ONE OF (semver)"),
    SEMVER_LESS(6, "< (semver)"),
    SEMVER_LESS_EQULAS(7, "<= (semver)"),
    SEMVER_GREATER(8, "> (semver)"),
    SEMVER_GREATER_EQUALS(9, ">= (semver)"),
    NUMBER_EQUALS(10, "= (number)"),
    NUMBER_NOT_EQUALS(11, "<> (number)"),
    NUMBER_LESS(12, "< (number)"),
    NUMBER_LESS_EQUALS(13, "<= (number)"),
    NUMBER_GREATER(14, "> (number)"),
    NUMBER_GREATER_EQUALS(15, ">= (number)"),
    SENSITIVE_IS_ONE_OF(16, "IS ONE OF (hashed)"),
    SENSITIVE_IS_NOT_ONE_OF(17, "IS NOT ONE OF (hashed)"),
    DATE_BEFORE(18, "BEFORE (UTC DateTime)"),
    DATE_AFTER(19, "AFTER (UTC DateTime)"),
    HASHED_EQUALS(20, "EQUALS (hashed)"),
    HASHED_NOT_EQUALS(21, "NOT EQUALS (hashed)"),
    HASHED_STARTS_WITH(22, "STARTS WITH ANY OF (hashed)"),
    HASHED_NOT_STARTS_WITH(23, "NOT STARTS WITH ANY OF (hashed)"),
    HASHED_ENDS_WITH(24, "ENDS WITH ANY OF (hashed)"),
    HASHED_NOT_ENDS_WITH(25, "NOT ENDS WITH ANY OF (hashed)"),
    HASHED_ARRAY_CONTAINS(26, "ARRAY CONTAINS (hashed)"),
    HASHED_ARRAY_NOT_CONTAINS(27, "ARRAY NOT CONTAINS (hashed)");

    private final int id;
    private final String name;

    Comparator(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Comparator fromId(int id) {
        for (Comparator comparator : Comparator.values()) {
            if (comparator.id == id) {
                return comparator;
            }
        }
        return null;
    }

}
