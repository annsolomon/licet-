package com.college.attendance;

/**
 * The five attendance statuses allowed by the CHECK constraint chk_att_status.
 * Only PRESENT counts towards the attendance percentage (Attendance % = PRESENT / TOTAL x 100).
 */
public enum AttendanceStatus {
    PRESENT, ABSENT, OD, MEDICAL, OTHER;

    public boolean countsAsPresent() {
        return this == PRESENT;
    }

    /** Parse user text safely; throws IllegalArgumentException with a friendly message. */
    public static AttendanceStatus parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Attendance status is required");
        }
        try {
            return valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid attendance status '" + text
                    + "'. Allowed: PRESENT, ABSENT, OD, MEDICAL, OTHER");
        }
    }
}
