package com.college.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Everything the dashboard needs in one call. */
public record DashboardStats(long students, long faculty, long subjects, long departments, long classes,
                             long employees, BigDecimal averageAttendance, long lowAttendanceStudents,
                             long passCount, long failCount, BigDecimal passPercentage,
                             Map<String, Long> studentsByDepartment, Map<String, Long> gradeDistribution,
                             List<String> flow) {
}
