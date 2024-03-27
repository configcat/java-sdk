package com.configcat;

/**
 * Prerequisite flag comparison operator used during the evaluation process.
 */
public enum PrerequisiteComparator {
    /**
     * EQUALS - Checks whether the evaluated value of the specified prerequisite flag is equal to the comparison value.
     */
    EQUALS(0, "EQUALS"),
    /**
     * NOT EQUALS - Checks whether the evaluated value of the specified prerequisite flag is not equal to the comparison value.
     */
    NOT_EQUALS(1, "NOT EQUALS");

    private final int id;
    private final String name;

    PrerequisiteComparator(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static PrerequisiteComparator fromId(int id) {
        for (PrerequisiteComparator comparator : PrerequisiteComparator.values()) {
            if (comparator.id == id) {
                return comparator;
            }
        }
        return null;
    }

}
