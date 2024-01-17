package com.configcat;

/**
 * Segment comparison operator used during the evaluation process.
 */
public enum SegmentComparator {
    /**
     * IS IN SEGMENT - It matches when the conditions of the specified segment are evaluated to true.
     */
    IS_IN_SEGMENT(0, "IS IN SEGMENT"),
    /**
     * IS NOT IN SEGMENT - It matches when the conditions of the specified segment are evaluated to false.
     */
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
