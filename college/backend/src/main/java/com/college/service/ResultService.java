package com.college.service;

import com.college.dto.GradeRequest;
import com.college.dto.ResultRequest;
import com.college.dto.SemesterGpa;
import com.college.dto.StudentResultSummary;
import com.college.exception.ResourceNotFoundException;
import com.college.exception.ValidationException;
import com.college.model.Student;
import com.college.model.ResultRow;
import com.college.repository.GradeRepository;
import com.college.repository.ResultRepository;
import com.college.repository.SubjectRepository;
import com.college.result.GradePointCalculator;
import com.college.result.GradePointCalculator.SubjectGrade;
import com.college.result.GradeRule;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RESULT flow:
 *
 *   React "Calculate" --POST /api/results/calculate--> ResultController --> ResultService
 *     --> ResultRepository.callCalculateStudentResult()  --> JdbcTemplate --> BEGIN calculate_student_result(?,?); END;
 *     --> Oracle: 7 raw marks -> internal /40 + semester /60 = final /100 -> GRADE lookup -> MERGE INTO result
 *     --> rows read back --> JSON --> React (grade, grade point, pass/fail, SGPA, CGPA)
 */
@Service
public class ResultService {

    private final ResultRepository repository;
    private final GradeRepository grades;
    private final StudentService students;
    private final SubjectRepository subjects;

    public ResultService(ResultRepository repository, GradeRepository grades,
                         StudentService students, SubjectRepository subjects) {
        this.repository = repository;
        this.grades = grades;
        this.students = students;
        this.subjects = subjects;
    }

    /** Oracle may answer with ORA-20010 ("marks incomplete"); GlobalExceptionHandler reports it as a PL/SQL error. */
    public List<ResultRow> calculate(ResultRequest r) {
        if (r == null || r.studentId() == null || r.subjectId() == null) {
            throw new ValidationException("studentId and subjectId are required");
        }
        students.requireExists(r.studentId());
        subjects.findById(r.subjectId()).orElseThrow(() -> new ResourceNotFoundException("Subject", r.subjectId()));
        repository.callCalculateStudentResult(r.studentId(), r.subjectId());
        return repository.findByStudent(r.studentId()).stream()
                .filter(row -> row.subjectId() == r.subjectId()).toList();
    }

    public int calculateAll() {
        return repository.callCalculateAllResults();
    }

    public List<ResultRow> list(Integer semester, Long departmentId) {
        if (semester != null) {
            return repository.findBySemester(semester);
        }
        if (departmentId != null) {
            return repository.findByDepartment(departmentId);
        }
        return repository.findAll();
    }

    /**
     * Results + SGPA per semester + CGPA. SGPA/CGPA come from the Oracle functions; "javaCgpa" is the same
     * number computed in Java (GradePointCalculator) from the result rows - they must be equal.
     */
    public StudentResultSummary summary(long studentId) {
        Student student = students.get(studentId);
        List<ResultRow> rows = repository.findByStudent(studentId);
        List<SemesterGpa> sgpa = repository.sgpaBySemester(studentId);
        BigDecimal cgpa = repository.oracleCgpa(studentId);
        List<SubjectGrade> forJava = new ArrayList<>();
        for (ResultRow row : rows) {
            forJava.add(new SubjectGrade(row.gradePoint(), row.credits()));
        }
        return new StudentResultSummary(studentId, student.getName(), rows, sgpa, cgpa, GradePointCalculator.gpa(forJava));
    }

    public List<Map<String, Object>> topPerformers(int limit) {
        return repository.topPerformers(Math.max(1, Math.min(limit, 50)));
    }

    // ------------------------------------------------------------------ configurable grade scale
    public List<GradeRule> grades() {
        return grades.findAll();
    }

    public List<GradeRule> updateGrade(String code, GradeRequest r) {
        if (r == null || r.minMark() == null || r.maxMark() == null || r.gradePoint() == null || r.pass() == null) {
            throw new ValidationException("minMark, maxMark, gradePoint and pass are required");
        }
        if (r.minMark().compareTo(r.maxMark()) > 0) {
            throw new ValidationException("minMark must not be greater than maxMark");
        }
        if (r.minMark().signum() < 0 || r.maxMark().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidationException("Marks must be between 0 and 100");
        }
        if (r.gradePoint().signum() < 0 || r.gradePoint().compareTo(BigDecimal.TEN) > 0) {
            throw new ValidationException("Grade point must be between 0 and 10");
        }
        if (grades.update(code.trim(), r.minMark(), r.maxMark(), r.gradePoint(), r.pass()) == 0) {
            throw new ResourceNotFoundException("Grade", code);
        }
        return grades.findAll();
    }
}
