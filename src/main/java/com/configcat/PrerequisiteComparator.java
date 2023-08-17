package com.configcat;

/**
 * Describes the Prerequisite Comparators.
 */
public enum PrerequisiteComparator {
    EQUALS(0, "EQUALS"),
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
