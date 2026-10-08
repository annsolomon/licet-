package com.college.dto;

import com.college.marks.MarkBreakdown;

import java.math.BigDecimal;

/**
 * The calculation shown twice: computed in JAVA (MarkCalculator -> "javaCalc") and read from ORACLE
 * (function calculate_internal_mark ...). "match" proves both agree.
 */
public record BreakdownResponse(long studentId, String studentName, String subjectCode,
                                MarkBreakdown javaCalc, BigDecimal oracleInternal, BigDecimal oracleSemester,
                                BigDecimal oracleFinal, String grade, boolean match, boolean complete,
                                String message) {
}
