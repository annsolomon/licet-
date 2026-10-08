package com.college.service;

import com.college.dto.DepartmentRequest;
import com.college.exception.ResourceNotFoundException;
import com.college.exception.ValidationException;
import com.college.model.Department;
import com.college.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DEPARTMENT business rules: validate -> repository.
 * Oracle remains the final judge: UNIQUE(dept_code) / UNIQUE(dept_name) reject duplicates (ORA-00001)
 * and the foreign keys stop deleting a department that still has students (ORA-02292).
 */
@Service
public class DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    public List<Department> findAll() {
        return repository.findAll();
    }

    public Department get(long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    public Department create(DepartmentRequest request) {
        String code = validCode(request);
        String name = validName(request);
        long id = repository.nextId();
        repository.insert(id, code, name);
        return get(id);
    }

    public Department update(long id, DepartmentRequest request) {
        get(id);                                   // 404 when missing
        repository.update(id, validCode(request), validName(request));
        return get(id);
    }

    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    private String validCode(DepartmentRequest r) {
        if (r == null || r.code() == null || !r.code().trim().matches("[A-Za-z]{2,10}")) {
            throw new ValidationException("Department code must be 2-10 letters, e.g. CSE");
        }
        return r.code().trim().toUpperCase();
    }

    private String validName(DepartmentRequest r) {
        if (r.name() == null || r.name().trim().length() < 3 || r.name().trim().length() > 100) {
            throw new ValidationException("Department name must be 3-100 characters");
        }
        return r.name().trim();
    }
}
