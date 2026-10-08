package com.college.service;

import com.college.dto.ClassRequest;
import com.college.dto.ClassSubjectRequest;
import com.college.exception.DuplicateResourceException;
import com.college.exception.ResourceNotFoundException;
import com.college.exception.ValidationException;
import com.college.model.ClassSection;
import com.college.model.ClassSubject;
import com.college.repository.ClassRepository;
import com.college.repository.DepartmentRepository;
import com.college.repository.FacultyRepository;
import com.college.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** CLASS (section) rules and the CLASS_SUBJECT assignments (who teaches what to whom). */
@Service
public class ClassService {

    private final ClassRepository repository;
    private final DepartmentRepository departments;
    private final SubjectRepository subjects;
    private final FacultyRepository faculty;

    public ClassService(ClassRepository repository, DepartmentRepository departments,
                        SubjectRepository subjects, FacultyRepository faculty) {
        this.repository = repository;
        this.departments = departments;
        this.subjects = subjects;
        this.faculty = faculty;
    }

    public List<ClassSection> findAll() {
        return repository.findAll();
    }

    public ClassSection get(long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Class", id));
    }

    public ClassSection create(ClassRequest r) {
        validate(r);
        long id = repository.nextId();
        repository.insert(id, r.name().trim(), r.departmentId(), r.semester(), r.section().trim().toUpperCase(), r.academicYear());
        return get(id);
    }

    public ClassSection update(long id, ClassRequest r) {
        get(id);
        validate(r);
        repository.update(id, r.name().trim(), r.departmentId(), r.semester(), r.section().trim().toUpperCase(), r.academicYear());
        return get(id);
    }

    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    // ---------------------------------------------------------------- CLASS_SUBJECT
    public List<ClassSubject> assignments(Long classId) {
        return classId == null ? repository.findAllAssignments() : repository.findAssignmentsByClass(classId);
    }

    public ClassSubject assign(ClassSubjectRequest r) {
        if (r == null || r.classId() == null || r.subjectId() == null || r.facultyId() == null) {
            throw new ValidationException("classId, subjectId and facultyId are required");
        }
        get(r.classId());
        subjects.findById(r.subjectId()).orElseThrow(() -> new ValidationException("Invalid subject " + r.subjectId()));
        faculty.findById(r.facultyId()).orElseThrow(() -> new ValidationException("Invalid faculty " + r.facultyId()));
        if (repository.findAssignment(r.classId(), r.subjectId()).isPresent()) {
            throw new DuplicateResourceException("This subject is already assigned to the class");
        }
        long id = repository.nextAssignmentId();
        repository.insertAssignment(id, r.classId(), r.subjectId(), r.facultyId());
        return repository.findAssignment(r.classId(), r.subjectId()).orElseThrow();
    }

    public void unassign(long assignmentId) {
        repository.deleteAssignment(assignmentId);
    }

    private void validate(ClassRequest r) {
        if (r == null || r.name() == null || r.name().trim().length() < 2) {
            throw new ValidationException("Class name is required (e.g. CSE-III-A)");
        }
        if (r.departmentId() == null) {
            throw new ValidationException("Department is required");
        }
        if (r.semester() == null || r.semester() < 1 || r.semester() > 8) {
            throw new ValidationException("Semester must be between 1 and 8");
        }
        if (r.section() == null || !r.section().trim().matches("[A-Za-z]{1,2}")) {
            throw new ValidationException("Section must be 1-2 letters, e.g. A");
        }
        departments.findById(r.departmentId())
                .orElseThrow(() -> new ValidationException("Department " + r.departmentId() + " does not exist"));
    }
}
