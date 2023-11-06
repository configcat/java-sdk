package com.configcat;

/**
 * Describes the Rollout Evaluator User Condition Comparators.
 */
public enum Comparator {

    IS_ONE_OF(0, "IS ONE OF"),
    IS_NOT_ONE_OF(1, "IS NOT ONE OF"),
    CONTAINS_ANY_OF(2, "CONTAINS ANY OF"),
    NOT_CONTAINS_ANY_OF(3, "NOT CONTAINS ANY OF"),
    SEMVER_IS_ONE_OF(4, "IS ONE OF"),
    SEMVER_IS_NOT_ONE_OF(5, "IS NOT ONE OF"),
    SEMVER_LESS(6, "<"),
    SEMVER_LESS_EQULAS(7, "<="),
    SEMVER_GREATER(8, ">"),
    SEMVER_GREATER_EQUALS(9, ">="),
    NUMBER_EQUALS(10, "="),
    NUMBER_NOT_EQUALS(11, "!="),
    NUMBER_LESS(12, "<"),
    NUMBER_LESS_EQUALS(13, "<="),
    NUMBER_GREATER(14, ">"),
    NUMBER_GREATER_EQUALS(15, ">="),
    SENSITIVE_IS_ONE_OF(16, "IS ONE OF"),
    SENSITIVE_IS_NOT_ONE_OF(17, "IS NOT ONE OF"),
    DATE_BEFORE(18, "BEFORE"),
    DATE_AFTER(19, "AFTER"),
    HASHED_EQUALS(20, "EQUALS"),
    HASHED_NOT_EQUALS(21, "NOT EQUALS"),
    HASHED_STARTS_WITH(22, "STARTS WITH ANY OF"),
    HASHED_NOT_STARTS_WITH(23, "NOT STARTS WITH ANY OF"),
    HASHED_ENDS_WITH(24, "ENDS WITH ANY OF"),
    HASHED_NOT_ENDS_WITH(25, "NOT ENDS WITH ANY OF"),
    HASHED_ARRAY_CONTAINS(26, "ARRAY CONTAINS ANY OF"),
    HASHED_ARRAY_NOT_CONTAINS(27, "ARRAY NOT CONTAINS ANY OF"),
    TEXT_EQUALS(28, "EQUALS"),
    TEXT_NOT_EQUALS(29, "NOT EQUALS"),
    TEXT_STARTS_WITH(30, "STARTS WITH ANY OF"),
    TEXT_NOT_STARTS_WITH(31, "NOT STARTS WITH ANY OF"),
    TEXT_ENDS_WITH(32, "ENDS WITH ANY OF"),
    TEXT_NOT_ENDS_WITH(33, "NOT ENDS WITH ANY OF"),
    TEXT_ARRAY_CONTAINS(34, "ARRAY CONTAINS ANY OF"),
    TEXT_ARRAY_NOT_CONTAINS(35, "ARRAY NOT CONTAINS ANY OF");

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
