package com.college.attendance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Attendance % = present / total x 100   (rounded to 2 decimals).
 * Same rule as the Oracle function calculate_attendance().
 */
public final class AttendanceCalculator {

    private AttendanceCalculator() { }

    /** @return percentage, or null when no session has been held (total = 0) */
    public static BigDecimal percentage(long present, long total) {
        if (total < 0 || present < 0 || present > total) {
            throw new IllegalArgumentException("Invalid attendance counts: present=" + present + " total=" + total);
        }
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(present)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /** true when the percentage is below the threshold (default policy: 75). */
    public static boolean isLow(BigDecimal percentage, int threshold) {
        return percentage != null && percentage.compareTo(BigDecimal.valueOf(threshold)) < 0;
    }
}
