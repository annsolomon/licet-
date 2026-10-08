package com.college.marks;

import com.college.exception.InvalidMarksException;

import java.math.BigDecimal;

/**
 * JAVA VALIDATION layer for marks (the 2nd line of defence; the 1st is the React form,
 * the 3rd is trigger trg_marks_validate and CHECK constraint chk_marks_nonneg in Oracle).
 */
public final class MarkValidator {

    private MarkValidator() { }

    /**
     * @param assessmentCode e.g. CT1
     * @param raw            the mark entered
     * @param max            maximum allowed for this assessment (from table ASSESSMENT)
     * @throws InvalidMarksException when raw is missing, negative or above max
     */
    public static void validate(String assessmentCode, BigDecimal raw, BigDecimal max) {
        if (raw == null) {
            throw new InvalidMarksException("Marks value is required for " + assessmentCode);
        }
        if (raw.signum() < 0) {
            throw new InvalidMarksException("Marks cannot be negative: " + raw);
        }
        if (raw.compareTo(max) > 0) {
            throw new InvalidMarksException("Marks " + raw.stripTrailingZeros().toPlainString()
                    + " exceed the maximum " + max.stripTrailingZeros().toPlainString()
                    + " for " + assessmentCode);
        }
    }
}
