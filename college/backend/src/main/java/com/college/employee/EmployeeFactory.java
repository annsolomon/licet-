package com.college.employee;

import java.math.BigDecimal;

/**
 * POLYMORPHISM in action: the database stores a designation TEXT; this factory turns it into
 * the right Employee SUBCLASS. The rest of the program only uses the parent type
 * (Employee / Payable) and Java calls the correct hra()/da()/pf() at run time.
 */
public final class EmployeeFactory {

    private EmployeeFactory() { }

    public static Employee create(long id, String name, String designation, BigDecimal basicSalary) {
        Designation d = Designation.parse(designation);
        return switch (d) {
            case PROGRAMMER          -> new Programmer(id, name, basicSalary);
            case ASSISTANT_PROFESSOR -> new AssistantProfessor(id, name, basicSalary);
            case ASSOCIATE_PROFESSOR -> new AssociateProfessor(id, name, basicSalary);
            case PROFESSOR           -> new Professor(id, name, basicSalary);
        };
    }
}
