package com.college.model;

import java.math.BigDecimal;

/** One RAW mark (row of MARKS joined with the assessment definition). */
public record MarkEntry(long id, long studentId, long subjectId, String subjectCode,
                        String assessmentCode, String assessmentName,
                        BigDecimal rawMarks, BigDecimal maxMarks) {
}
