package com.college.service;

import com.college.dto.StudentRequest;
import com.college.exception.DuplicateResourceException;
import com.college.exception.InvalidStudentException;
import com.college.exception.StudentNotFoundException;
import com.college.model.ClassSection;
import com.college.model.Student;
import com.college.repository.ClassRepository;
import com.college.repository.DepartmentRepository;
import com.college.repository.StudentRepository;
import com.college.util.GenericSearch;
import com.college.util.Searchable;
import com.college.util.StringUtil;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

/**
 * STUDENT creation flow (read it top to bottom in the viva):
 *
 *   React form --POST /api/students--> StudentController
 *        --> StudentService.create():   1 VALIDATION (Java)   2 duplicate checks   3 id from sequence
 *        --> StudentRepository.insert() --> JdbcTemplate.update() --> INSERT INTO student ...
 *        --> Oracle (constraints + INSERT trigger -> AUDIT_LOG) --> row read back --> JSON --> React table
 *
 * Implements Searchable&lt;Student&gt; (interface) and uses GenericSearch&lt;Student&gt; (generics) + List (collections).
 */
@Service
public class StudentService implements Searchable<Student> {

    private final StudentRepository repository;
    private final DepartmentRepository departments;
    private final ClassRepository classes;
    private final GenericSearch<Student> genericSearch = new GenericSearch<>();

    public StudentService(StudentRepository repository, DepartmentRepository departments, ClassRepository classes) {
        this.repository = repository;
        this.departments = departments;
        this.classes = classes;
    }

    public List<Student> findAll() {
        return repository.findAll();
    }

    public Student get(long id) {
        return repository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public List<Student> findByDepartment(long deptId) {
        return repository.findByDepartment(deptId);
    }

    public List<Student> findByClass(long classId) {
        return repository.findByClass(classId);
    }

    /**
     * Searchable contract, implemented with the GENERIC search:
     * load all students into a List and keep those whose id or name contains the keyword (Java does the search).
     */
    @Override
    public List<Student> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return repository.findAll();
        }
        String k = keyword.trim();
        return genericSearch.filter(repository.findAll(),
                s -> StringUtil.containsIgnoreCase(s.getName(), k) || String.valueOf(s.getId()).contains(k));
    }

    /** Same search done by ORACLE with a WHERE ... LIKE ? query. */
    public List<Student> searchSql(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return repository.findAll();
        }
        return repository.searchBySql(keyword);
    }

    /**
     * @param skipDuplicateChecks demo switch: when true, Java does NOT check duplicates, so the request goes
     *                            to Oracle and the UNIQUE / PRIMARY KEY constraint rejects it (ORA-00001).
     */
    public Student create(StudentRequest request, boolean skipDuplicateChecks) {
        Clean c = validate(request);
        long id = request.id() != null ? request.id() : repository.nextId();
        if (id <= 0) {
            throw new InvalidStudentException("Student id must be a positive number");
        }
        if (!skipDuplicateChecks) {
            if (repository.existsById(id)) {
                throw new DuplicateResourceException("Student id " + id + " already exists");
            }
            if (repository.existsByEmail(c.email)) {
                throw new DuplicateResourceException("E-mail " + c.email + " is already used by another student");
            }
        }
        repository.insert(id, c.name, c.email, c.phone, c.deptId, c.classId, c.joinedYear);
        return get(id);
    }

    public Student update(long id, StudentRequest request) {
        get(id);
        Clean c = validate(request);
        repository.update(id, c.name, c.email, c.phone, c.deptId, c.classId, c.joinedYear);
        return get(id);
    }

    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    // ------------------------------------------------------------------ validation (Java layer)
    private record Clean(String name, String email, String phone, long deptId, long classId, Integer joinedYear) { }

    private Clean validate(StudentRequest r) {
        if (r == null) {
            throw new InvalidStudentException("Student data is required");
        }
        if (!StringUtil.isValidName(r.name())) {
            throw new InvalidStudentException("Name must be 2-100 letters (spaces, dots and apostrophes allowed)");
        }
        if (!StringUtil.isValidEmail(r.email())) {
            throw new InvalidStudentException("E-mail address is not valid");
        }
        if (r.phone() != null && !r.phone().isBlank() && !r.phone().matches("[0-9]{10}")) {
            throw new InvalidStudentException("Phone must be exactly 10 digits");
        }
        if (r.departmentId() == null) {
            throw new InvalidStudentException("Department is required");
        }
        if (r.classId() == null) {
            throw new InvalidStudentException("Class is required");
        }
        if (r.joinedYear() != null && (r.joinedYear() < 2000 || r.joinedYear() > Year.now().getValue() + 1)) {
            throw new InvalidStudentException("Joined year must be between 2000 and " + (Year.now().getValue() + 1));
        }
        departments.findById(r.departmentId()).orElseThrow(() -> new InvalidStudentException(
                "Department " + r.departmentId() + " does not exist"));
        ClassSection section = classes.findById(r.classId()).orElseThrow(() -> new InvalidStudentException(
                "Class " + r.classId() + " does not exist"));
        if (section.deptId() != r.departmentId()) {
            throw new InvalidStudentException("Class " + section.name() + " does not belong to the chosen department");
        }
        String phone = (r.phone() == null || r.phone().isBlank()) ? null : r.phone().trim();
        return new Clean(StringUtil.toTitleCase(r.name()), r.email().trim().toLowerCase(), phone,
                r.departmentId(), r.classId(), r.joinedYear());
    }

    /** Used by other services that need an existing student or a clear 404. */
    public void requireExists(long studentId) {
        if (!repository.existsById(studentId)) {
            throw new StudentNotFoundException(studentId);
        }
    }
}
