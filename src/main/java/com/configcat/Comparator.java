package com.configcat;

/**
 * Describes the Rollout Evaluator Comparators.
 */
public enum Comparator {
    CONTAINS(2, "CONTAINS"),
    DOES_NOT_CONTAIN(3, "DOES NOT CONTAIN"),
    SEMVER_IS_ONE_OF(4, "IS ONE OF (SemVer)"),
    SEMVER_IS_NOT_ONE_OF(5, "IS NOT ONE OF (SemVer)"),
    SEMVER_LESS(6, "< (SemVer)"),
    SEMVER_LESS_EQULAS(7, "<= (SemVer)"),
    SEMVER_GREATER(8, "> (SemVer)"),
    SEMVER_GREATER_EQUALS(9, ">= (SemVer)"),
    NUMBER_EQUALS(10, "= (Number)"),
    NUMBER_NOT_EQUALS(11, "<> (Number)"),
    NUMBER_LESS(12, "< (Number)"),
    NUMBER_LESS_EQUALS(13, "<= (Number)"),
    NUMBER_GREATER(14, "> (Number)"),
    NUMBER_GREATER_EQUALS(15, ">= (Number)"),
    SENSITIVE_IS_ONE_OF(16, "IS ONE OF (Sensitive)"),
    SENSITIVE_IS_NOT_ONE_OF(17, "IS NOT ONE OF (Sensitive)"),
    DATE_BEFORE(18, "BEFORE (UTC DateTime)"),
    DATE_AFTER(19, "AFTER (UTC DateTime)"),
    HASHED_EQUALS(20, "EQUALS (hashed)"),
    HASHED_NOT_EQUALS(21, "NOT EQUALS (hashed)"),
    HASHED_STARTS_WITH(22, "STARTS WITH ANY OF (hashed)"),
    HASHED_ENDS_WITH(23, "ENDS WITH ANY OF (hashed)"),
    HASHED_ARRAY_CONTAINS(24, "ARRAY CONTAINS (hashed)"),
    HASHED_ARRAY_NOT_CONTAINS(25, "ARRAY NOT CONTAINS (hashed)");

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
