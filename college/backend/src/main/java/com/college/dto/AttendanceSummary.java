package com.college.dto;

import java.math.BigDecimal;

/** Attendance of one student in one subject (or in all subjects when subjectCode is "ALL"). */
public record AttendanceSummary(long studentId, String studentName, String subjectCode,
                                long total, long present, BigDecimal percentage, boolean low) {
}
