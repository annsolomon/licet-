package com.college.service;

import com.college.dto.SubjectRequest;
import com.college.exception.ResourceNotFoundException;
import com.college.exception.ValidationException;
import com.college.model.Subject;
import com.college.repository.DepartmentRepository;
import com.college.repository.SubjectRepository;
import com.college.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/** SUBJECT rules. String handling in action: the code is trimmed, upper-cased and pattern-checked (CS302). */
@Service
public class SubjectService {

    private final SubjectRepository repository;
    private final DepartmentRepository departments;

    public SubjectService(SubjectRepository repository, DepartmentRepository departments) {
        this.repository = repository;
        this.departments = departments;
    }

    public List<Subject> findAll() {
        return repository.findAll();
    }

    public Subject get(long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Subject", id));
    }

    public Subject create(SubjectRequest r) {
        String code = validate(r);
        long id = repository.nextId();
        repository.insert(id, code, r.name().trim(), r.credits(), r.semester(), r.departmentId());
        return get(id);
    }

    public Subject update(long id, SubjectRequest r) {
        get(id);
        String code = validate(r);
        repository.update(id, code, r.name().trim(), r.credits(), r.semester(), r.departmentId());
        return get(id);
    }

    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    private String validate(SubjectRequest r) {
        if (r == null) {
            throw new ValidationException("Subject data is required");
        }
        String code = StringUtil.normalizeSubjectCode(r.code());
        if (!StringUtil.isValidSubjectCode(code)) {
            throw new ValidationException("Subject code must be 2-3 letters followed by 3 digits, e.g. CS302");
        }
        if (r.name() == null || r.name().trim().length() < 3) {
            throw new ValidationException("Subject name must have at least 3 characters");
        }
        if (r.credits() == null || r.credits() < 1 || r.credits() > 6) {
            throw new ValidationException("Credits must be between 1 and 6");
        }
        if (r.semester() == null || r.semester() < 1 || r.semester() > 8) {
            throw new ValidationException("Semester must be between 1 and 8");
        }
        if (r.departmentId() == null) {
            throw new ValidationException("Department is required");
        }
        departments.findById(r.departmentId())
                .orElseThrow(() -> new ValidationException("Department " + r.departmentId() + " does not exist"));
        return code;
    }
}
