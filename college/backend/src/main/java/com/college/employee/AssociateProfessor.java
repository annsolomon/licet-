package com.college.employee;

import java.math.BigDecimal;

/** Associate Professor: HRA 18%, DA 9%, PF 12%. */
public class AssociateProfessor extends Employee {

    public AssociateProfessor(long id, String name, BigDecimal basicSalary) {
        super(id, name, basicSalary);
    }

    @Override public Designation designation() { return Designation.ASSOCIATE_PROFESSOR; }
    @Override protected int hraPercent() { return 18; }
    @Override protected int daPercent()  { return 9; }
    @Override protected int pfPercent()  { return 12; }
}
