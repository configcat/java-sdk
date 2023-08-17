package com.configcat;

/**
 * Describes the Segment Comparators.
 */
public enum SegmentComparator {
    IS_IN_SEGMENT(0, "IS IN SEGMENT"),
    IS_NOT_IN_SEGMENT(1, "IS NOT IN SEGMENT");

    private final int id;
    private final String name;

    SegmentComparator(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SegmentComparator fromId(int id) {
        for (SegmentComparator comparator : SegmentComparator.values()) {
            if (comparator.id == id) {
                return comparator;
            }
        }
        return null;
    }

}
