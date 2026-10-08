package com.college.dto;

import com.college.model.ResultRow;

import java.math.BigDecimal;
import java.util.List;

/** All results of a student + SGPA per semester + CGPA (SGPA/CGPA come from the Oracle functions). */
public record StudentResultSummary(long studentId, String studentName,
                                   List<ResultRow> results, List<SemesterGpa> sgpa, BigDecimal cgpa,
                                   BigDecimal javaCgpa) {
}
