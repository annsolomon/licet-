package com.college.payroll;

import java.util.ArrayList;
import java.util.List;

/** Builds the printable text of a payslip (shown in the app and exported to a .txt file). */
public final class PayslipFormatter {

    private PayslipFormatter() { }

    public static List<String> toLines(Payslip p) {
        List<String> lines = new ArrayList<>();
        lines.add("==================================================");
        lines.add("            COLLEGE ACADEMIC MANAGEMENT SYSTEM");
        lines.add("                     PAYSLIP  " + p.month());
        lines.add("==================================================");
        lines.add(String.format("Employee ID  : %d", p.empId()));
        lines.add(String.format("Name         : %s", p.name()));
        lines.add(String.format("Designation  : %s", p.designation()));
        lines.add("--------------------------------------------------");
        lines.add(String.format("Basic salary         : Rs. %12s", p.basic().toPlainString()));
        lines.add(String.format("+ HRA                : Rs. %12s", p.hra().toPlainString()));
        lines.add(String.format("+ DA                 : Rs. %12s", p.da().toPlainString()));
        lines.add(String.format("= GROSS salary       : Rs. %12s", p.gross().toPlainString()));
        lines.add(String.format("- PF (deduction)     : Rs. %12s", p.pf().toPlainString()));
        lines.add("--------------------------------------------------");
        lines.add(String.format("NET salary           : Rs. %12s", p.net().toPlainString()));
        lines.add("==================================================");
        return lines;
    }
}
