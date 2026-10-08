package com.college.model;

import java.math.BigDecimal;

/** One row of RESULT joined with student / subject names. */
public record ResultRow(long id, long studentId, String studentName, String deptCode,
                        long subjectId, String subjectCode, String subjectName, int semester,
                        BigDecimal internalMark, BigDecimal semesterMark, BigDecimal finalMark,
                        String grade, BigDecimal gradePoint, int credits, String passFail) {
}
