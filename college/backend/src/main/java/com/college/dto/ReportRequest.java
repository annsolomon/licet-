package com.college.dto;

import java.util.List;

/** Ask for one report (type = STUDENT, ATTENDANCE, MARKS, RESULT, DEPARTMENT, PAYROLL, LOW_ATTENDANCE, TOP_PERFORMERS). */
public record ReportRequest(String type, Long departmentId, String month, Integer threshold) {

    /** Used by the multi-threaded generator: several types at once. */
    public record Batch(List<String> types) { }
}
