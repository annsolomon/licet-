package com.college.model;

import java.math.BigDecimal;

/** One row of PAYROLL joined with the employee name. */
public record PayrollRecord(long id, long empId, String empName, String designation, String month,
                            BigDecimal basic, BigDecimal hra, BigDecimal da, BigDecimal pf,
                            BigDecimal gross, BigDecimal net) {
}
