package com.college.employee;

import java.math.BigDecimal;

/** Assistant Professor: HRA 15%, DA 8%, PF 12%. */
public class AssistantProfessor extends Employee {

    public AssistantProfessor(long id, String name, BigDecimal basicSalary) {
        super(id, name, basicSalary);
    }

    @Override public Designation designation() { return Designation.ASSISTANT_PROFESSOR; }
    @Override protected int hraPercent() { return 15; }
    @Override protected int daPercent()  { return 8; }
    @Override protected int pfPercent()  { return 12; }
}
