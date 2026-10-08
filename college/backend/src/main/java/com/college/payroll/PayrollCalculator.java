package com.college.payroll;

import com.college.employee.Employee;

/**
 * Turns an Employee (any subclass) into a Payslip.
 * Because Employee implements Payable, this class never needs to know WHICH subclass it got:
 * employee.hra() runs the right code for Programmer / Professor / ... (polymorphism).
 */
public final class PayrollCalculator {

    private PayrollCalculator() { }

    public static Payslip payslip(Employee employee, String month) {
        return new Payslip(
                employee.getId(),
                employee.getName(),
                employee.designation().label(),
                month,
                employee.basic(),
                employee.hra(),
                employee.da(),
                employee.pf(),
                employee.gross(),
                employee.net());
    }
}
