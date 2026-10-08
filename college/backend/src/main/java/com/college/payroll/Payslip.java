package com.college.payroll;

import java.math.BigDecimal;

/**
 * A payslip as plain data. A Java "record" is a short way to declare an immutable data class:
 * the compiler creates the constructor, getters (empId(), name() ...), equals, hashCode, toString.
 * Spring's JSON library turns it into JSON automatically.
 */
public record Payslip(
        long empId,
        String name,
        String designation,
        String month,
        BigDecimal basic,
        BigDecimal hra,
        BigDecimal da,
        BigDecimal pf,
        BigDecimal gross,
        BigDecimal net) {
}
