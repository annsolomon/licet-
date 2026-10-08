package com.college.employee;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SYLLABUS: INHERITANCE + ABSTRACT CLASS + INTERFACE (Employee payslip).
 *
 *            Payable (interface)
 *               ^
 *               | implements
 *            Employee (abstract)  id, name, designation, basicSalary
 *               ^
 *     ----------+---------------------------------
 *     |            |                 |            |
 *  Programmer  AssistantProfessor  AssociateProfessor  Professor
 *
 * The parent holds everything COMMON (id, name, basic salary, formulas).
 * Each child only says WHICH percentages apply to it - the "extends" keyword gives it the rest.
 *
 * Example (Professor, basic 80000, HRA 20%, DA 10%, PF 12%):
 *   HRA 16000, DA 8000, PF 9600,  gross = 104000,  net = 94400
 */
public abstract class Employee implements Payable {

    private final long id;
    private final String name;
    private final BigDecimal basicSalary;

    protected Employee(long id, String name, BigDecimal basicSalary) {
        this.id = id;
        this.name = name;
        this.basicSalary = basicSalary.setScale(2, RoundingMode.HALF_UP);
    }

    // ---- what each subclass MUST provide (overriding) ----
    public abstract Designation designation();
    protected abstract int hraPercent();
    protected abstract int daPercent();
    protected abstract int pfPercent();

    // ---- common behaviour (inherited by all children) ----
    public long getId()              { return id; }
    public String getName()          { return name; }

    @Override public BigDecimal basic() { return basicSalary; }
    @Override public BigDecimal hra()   { return percentOfBasic(hraPercent()); }
    @Override public BigDecimal da()    { return percentOfBasic(daPercent()); }
    @Override public BigDecimal pf()    { return percentOfBasic(pfPercent()); }

    private BigDecimal percentOfBasic(int percent) {
        return basicSalary.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return designation().label() + "[" + id + ", " + name + ", basic=" + basicSalary + "]";
    }
}
