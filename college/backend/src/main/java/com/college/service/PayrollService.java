package com.college.service;

import com.college.dto.PayrollRequest;
import com.college.exception.DuplicateResourceException;
import com.college.exception.ResourceNotFoundException;
import com.college.exception.ValidationException;
import com.college.model.EmployeeRecord;
import com.college.model.PayrollRecord;
import com.college.payroll.Payslip;
import com.college.payroll.PayslipFormatter;
import com.college.repository.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PAYROLL flow - two ways to do the same job (both are demonstrated):
 *
 *   JAVA / OOP path    : EmployeeService.toEmployee()  -> Professor.hra()/da()/pf() -> Payslip -> INSERT INTO payroll
 *   PL/SQL path        : JdbcTemplate -> CALL generate_payroll(emp, month, OUT id) -> Oracle does the maths and the INSERT
 *
 * UNIQUE(emp_id, pay_month) makes a second payroll for the same month impossible (duplicate payroll error).
 */
@Service
public class PayrollService {

    private static final Pattern MONTH = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    private final PayrollRepository repository;
    private final EmployeeService employees;

    public PayrollService(PayrollRepository repository, EmployeeService employees) {
        this.repository = repository;
        this.employees = employees;
    }

    public List<PayrollRecord> list(String month) {
        if (month == null || month.isBlank()) {
            return repository.findAll();
        }
        return repository.findByMonth(validMonth(month));
    }

    public PayrollRecord get(long payrollId) {
        return repository.findById(payrollId).orElseThrow(() -> new ResourceNotFoundException("Payroll", payrollId));
    }

    /** Java path for one employee. */
    public PayrollRecord generateJava(PayrollRequest r) {
        if (r == null || r.empId() == null) {
            throw new ValidationException("empId is required");
        }
        String month = validMonth(r.month());
        if (repository.exists(r.empId(), month)) {
            throw new DuplicateResourceException("Payroll already generated for employee " + r.empId() + " for " + month);
        }
        Payslip payslip = employees.payslip(r.empId(), month);
        long id = repository.insert(payslip);
        return get(id);
    }

    /** Java path for every employee that does not yet have this month. */
    @Transactional
    public Map<String, Object> generateAllJava(String monthText) {
        String month = validMonth(monthText);
        int created = 0;
        int skipped = 0;
        for (EmployeeRecord e : employees.findAll()) {
            if (repository.exists(e.id(), month)) {
                skipped++;
            } else {
                repository.insert(employees.payslip(e.id(), month));
                created++;
            }
        }
        return Map.of("month", month, "created", created, "skipped", skipped, "via", "Java OOP (Employee subclasses)");
    }

    /** PL/SQL path for one employee: Oracle raises ORA-20020 on a duplicate. */
    public PayrollRecord generatePlsql(PayrollRequest r) {
        if (r == null || r.empId() == null) {
            throw new ValidationException("empId is required");
        }
        employees.get(r.empId());
        long id = repository.callGeneratePayroll(r.empId(), validMonth(r.month()));
        return get(id);
    }

    public Map<String, Object> generateAllPlsql(String monthText) {
        String month = validMonth(monthText);
        int created = repository.callGeneratePayrollAll(month);
        return Map.of("month", month, "created", created, "via", "PL/SQL procedure generate_payroll_all");
    }

    /** Printable payslip lines (shown on the Payslips page and exported to a file). */
    public List<String> payslipLines(long payrollId) {
        PayrollRecord p = get(payrollId);
        Payslip payslip = new Payslip(p.empId(), p.empName(), labelOf(p.designation()), p.month(),
                p.basic(), p.hra(), p.da(), p.pf(), p.gross(), p.net());
        return PayslipFormatter.toLines(payslip);
    }

    private static String labelOf(String dbDesignation) {
        return com.college.employee.Designation.parse(dbDesignation).label();
    }

    public int deleteMonth(String month) {
        return repository.deleteByMonth(validMonth(month));
    }

    private String validMonth(String month) {
        if (month == null || !MONTH.matcher(month.trim()).matches()) {
            throw new ValidationException("Month must look like 2026-10 (YYYY-MM)");
        }
        return month.trim();
    }
}
