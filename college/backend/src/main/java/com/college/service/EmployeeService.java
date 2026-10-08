package com.college.service;

import com.college.dto.EmployeeRequest;
import com.college.employee.Designation;
import com.college.employee.Employee;
import com.college.employee.EmployeeFactory;
import com.college.exception.DuplicateResourceException;
import com.college.exception.ResourceNotFoundException;
import com.college.exception.ValidationException;
import com.college.model.EmployeeRecord;
import com.college.payroll.PayrollCalculator;
import com.college.payroll.Payslip;
import com.college.repository.EmployeeRepository;
import com.college.util.StringUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** EMPLOYEE rules + conversion of a database row into the right Employee SUBCLASS (polymorphism). */
@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public List<EmployeeRecord> findAll() {
        return repository.findAll();
    }

    public EmployeeRecord get(long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    public EmployeeRecord create(EmployeeRequest r) {
        Designation d = validate(r);
        long id = r.id() != null ? r.id() : repository.nextId();
        if (r.id() != null && repository.findById(id).isPresent()) {
            throw new DuplicateResourceException("Employee id " + id + " already exists");
        }
        repository.insert(id, r.name().trim(), r.email().trim().toLowerCase(), d.name(), r.basicSalary());
        return get(id);
    }

    public EmployeeRecord update(long id, EmployeeRequest r) {
        get(id);
        Designation d = validate(r);
        repository.update(id, r.name().trim(), r.email().trim().toLowerCase(), d.name(), r.basicSalary());
        return get(id);
    }

    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    /** DB row -> Employee object of the correct subclass (Professor, Programmer ...). */
    public Employee toEmployee(EmployeeRecord rec) {
        return EmployeeFactory.create(rec.id(), rec.name(), rec.designation(), rec.basicSalary());
    }

    /** The payslip is calculated by the OOP classes (Employee.hra(), da(), pf(), gross(), net()). */
    public Payslip payslip(long empId, String month) {
        return PayrollCalculator.payslip(toEmployee(get(empId)), month);
    }

    private Designation validate(EmployeeRequest r) {
        if (r == null || !StringUtil.isValidName(r.name())) {
            throw new ValidationException("Employee name must be 2-100 letters (dots allowed, e.g. Dr. Ramesh Iyer)");
        }
        if (!StringUtil.isValidEmail(r.email())) {
            throw new ValidationException("E-mail address is not valid");
        }
        Designation d;
        try {
            d = Designation.parse(r.designation());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        if (r.basicSalary() == null || r.basicSalary().compareTo(BigDecimal.ZERO) <= 0
                || r.basicSalary().compareTo(new BigDecimal("1000000")) > 0) {
            throw new ValidationException("Basic salary must be between 1 and 1,000,000");
        }
        return d;
    }
}
