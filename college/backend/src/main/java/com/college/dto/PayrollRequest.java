package com.college.dto;

/** month format: YYYY-MM, e.g. 2026-10. empId may be null for "generate for everybody". */
public record PayrollRequest(Long empId, String month) {
}
