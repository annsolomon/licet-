package com.college.model;

import java.math.BigDecimal;

/** One row of EMPLOYEE (the data). The behaviour (salary rules) lives in com.college.employee.Employee. */
public record EmployeeRecord(long id, String name, String email, String designation, BigDecimal basicSalary) {
}
