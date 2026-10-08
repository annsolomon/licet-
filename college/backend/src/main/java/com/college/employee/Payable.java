package com.college.employee;

import java.math.BigDecimal;

/**
 * SYLLABUS: INTERFACE.
 *
 * Contract for anything that receives a salary. Employee implements it;
 * a future class (e.g. VisitingFaculty) could implement it too.
 *
 * "default" methods are methods that already have a body inside the interface:
 * gross() and net() are calculated from the other values.
 */
public interface Payable {

    BigDecimal basic();
    BigDecimal hra();       // House Rent Allowance
    BigDecimal da();        // Dearness Allowance
    BigDecimal pf();        // Provident Fund deduction

    /** Gross salary = basic + HRA + DA */
    default BigDecimal gross() {
        return basic().add(hra()).add(da());
    }

    /** Net salary = gross - PF */
    default BigDecimal net() {
        return gross().subtract(pf());
    }
}
