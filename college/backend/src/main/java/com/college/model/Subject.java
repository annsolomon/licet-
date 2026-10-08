package com.college.model;

/** One row of SUBJECT (with the department code joined in for display). */
public record Subject(long id, String code, String name, int credits, int semester, long deptId, String deptCode) {
}
