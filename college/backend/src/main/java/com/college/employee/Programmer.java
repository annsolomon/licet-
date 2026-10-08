package com.college.employee;

import java.math.BigDecimal;

/** Programmer: HRA 10%, DA 5%, PF 12%  (same numbers as table SALARY_RULE). */
public class Programmer extends Employee {

    public Programmer(long id, String name, BigDecimal basicSalary) {
        super(id, name, basicSalary);
    }

    @Override public Designation designation() { return Designation.PROGRAMMER; }
    @Override protected int hraPercent() { return 10; }
    @Override protected int daPercent()  { return 5; }
    @Override protected int pfPercent()  { return 12; }
}
