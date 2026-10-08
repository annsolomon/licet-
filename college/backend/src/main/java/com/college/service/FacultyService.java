package com.college.service;

import com.college.dto.FacultyRequest;
import com.college.exception.ResourceNotFoundException;
import com.college.exception.ValidationException;
import com.college.model.Faculty;
import com.college.repository.DepartmentRepository;
import com.college.repository.FacultyRepository;
import com.college.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/** FACULTY rules: code like F001, valid name and e-mail, existing department. */
@Service
public class FacultyService {

    private final FacultyRepository repository;
    private final DepartmentRepository departments;

    public FacultyService(FacultyRepository repository, DepartmentRepository departments) {
        this.repository = repository;
        this.departments = departments;
    }

    public List<Faculty> findAll() {
        return repository.findAll();
    }

    public Faculty get(long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Faculty", id));
    }

    public Faculty create(FacultyRequest r) {
        validate(r);
        long id = repository.nextId();
        repository.insert(id, r.code().trim().toUpperCase(), StringUtil.toTitleCase(r.name()),
                r.email().trim().toLowerCase(), clean(r.designation()), r.departmentId());
        return get(id);
    }

    public Faculty update(long id, FacultyRequest r) {
        get(id);
        validate(r);
        repository.update(id, r.code().trim().toUpperCase(), StringUtil.toTitleCase(r.name()),
                r.email().trim().toLowerCase(), clean(r.designation()), r.departmentId());
        return get(id);
    }

    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    private void validate(FacultyRequest r) {
        if (r == null || r.code() == null || !r.code().trim().matches("[A-Za-z][0-9]{3}")) {
            throw new ValidationException("Faculty code must be a letter and 3 digits, e.g. F007");
        }
        if (!StringUtil.isValidName(r.name())) {
            throw new ValidationException("Faculty name must be 2-100 letters");
        }
        if (!StringUtil.isValidEmail(r.email())) {
            throw new ValidationException("E-mail address is not valid");
        }
        if (r.departmentId() == null) {
            throw new ValidationException("Department is required");
        }
        departments.findById(r.departmentId())
                .orElseThrow(() -> new ValidationException("Department " + r.departmentId() + " does not exist"));
    }

    private String clean(String designation) {
        return designation == null || designation.isBlank() ? null : designation.trim();
    }
}
