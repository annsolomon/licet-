package com.college.model;

/** One row of CLASS (named ClassSection because "Class" is a Java keyword type). */
public record ClassSection(long id, String name, long deptId, String deptCode,
                           int semester, String section, String academicYear, int studentCount) {
}
