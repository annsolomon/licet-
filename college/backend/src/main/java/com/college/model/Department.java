package com.college.model;

/** One row of DEPARTMENT. A record: immutable data holder (see Payslip for the explanation). */
public record Department(long id, String code, String name, int studentCount) {
}
