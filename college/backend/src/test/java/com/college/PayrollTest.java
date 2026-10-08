package com.college;

import com.college.employee.Employee;
import com.college.employee.EmployeeFactory;
import com.college.payroll.PayrollCalculator;
import com.college.payroll.Payslip;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollTest {

    @Test
    void professorExample() {
        Employee e = EmployeeFactory.create(1, "Dr. Test", "PROFESSOR", new BigDecimal("80000"));
        Payslip p = PayrollCalculator.payslip(e, "2026-10");
        assertEquals(0, new BigDecimal("104000").compareTo(p.gross()));
        assertEquals(0, new BigDecimal("94400").compareTo(p.net()));
    }

    @Test
    void everyDesignationHasNetBelowGross() {
        for (String d : new String[]{"PROGRAMMER", "ASSISTANT_PROFESSOR", "ASSOCIATE_PROFESSOR", "PROFESSOR"}) {
            Payslip p = PayrollCalculator.payslip(EmployeeFactory.create(1, "X Y", d, new BigDecimal("50000")), "2026-10");
            assertTrue(p.net().compareTo(p.gross()) < 0, d);
        }
    }

    @Test
    void unknownDesignationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> EmployeeFactory.create(1, "X Y", "CLERK", new BigDecimal("1000")));
    }
}
