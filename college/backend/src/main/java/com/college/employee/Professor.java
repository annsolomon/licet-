package com.college.employee;

import java.math.BigDecimal;

/** Professor: HRA 20%, DA 10%, PF 12%.  basic 80000 -> HRA 16000, DA 8000, PF 9600. */
public class Professor extends Employee {

    public Professor(long id, String name, BigDecimal basicSalary) {
        super(id, name, basicSalary);
    }

    @Override public Designation designation() { return Designation.PROFESSOR; }
    @Override protected int hraPercent() { return 20; }
    @Override protected int daPercent()  { return 10; }
    @Override protected int pfPercent()  { return 12; }
}
