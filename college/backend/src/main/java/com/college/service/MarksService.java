package com.college.service;

import com.college.dto.BreakdownResponse;
import com.college.dto.MarkRequest;
import com.college.exception.ResourceNotFoundException;
import com.college.exception.ValidationException;
import com.college.marks.MarkBreakdown;
import com.college.marks.MarkCalculator;
import com.college.marks.MarkValidator;
import com.college.model.Assessment;
import com.college.model.MarkEntry;
import com.college.model.Student;
import com.college.model.Subject;
import com.college.repository.GradeRepository;
import com.college.repository.MarksRepository;
import com.college.repository.SubjectRepository;
import com.college.result.GradeRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * MARKS flow:
 *
 *   React marks form --PUT /api/marks--> MarksController --> MarksService.save()
 *      1 Java validation (MarkValidator: not negative, not above maximum)
 *      2 DBMS_SESSION.SET_IDENTIFIER(user)         (so the audit trigger knows WHO)
 *      3 MERGE INTO marks ...                      (raw mark stored - never overwritten by converted marks)
 *      4 Oracle triggers: trg_marks_validate (again!) + trg_marks_audit_update -> AUDIT_LOG
 *   then:  calculation (breakdown) --> POST /api/results/calculate --> RESULT --> React
 */
@Service
public class MarksService {

    private final MarksRepository repository;
    private final StudentService students;
    private final SubjectRepository subjects;
    private final GradeRepository grades;

    public MarksService(MarksRepository repository, StudentService students,
                        SubjectRepository subjects, GradeRepository grades) {
        this.repository = repository;
        this.students = students;
        this.subjects = subjects;
        this.grades = grades;
    }

    public List<Assessment> assessments() {
        return repository.findAssessments();
    }

    public List<MarkEntry> marks(long studentId, Long subjectId) {
        students.requireExists(studentId);
        return repository.findMarks(studentId, subjectId);
    }

    /**
     * Saves one raw mark.
     * @param skipJavaValidation demo switch: send the value straight to Oracle so the TRIGGER (ORA-20001) or the
     *                           CHECK constraint (ORA-02290) rejects it instead of Java.
     */
    @Transactional
    public List<MarkEntry> save(MarkRequest r, String username, boolean skipJavaValidation) {
        if (r == null || r.studentId() == null || r.subjectId() == null || r.assessmentCode() == null) {
            throw new ValidationException("studentId, subjectId and assessmentCode are required");
        }
        students.requireExists(r.studentId());
        subjects.findById(r.subjectId()).orElseThrow(() -> new ValidationException("Invalid subject " + r.subjectId()));
        Assessment assessment = repository.findAssessmentByCode(r.assessmentCode().trim().toUpperCase())
                .orElseThrow(() -> new ValidationException("Invalid assessment code '" + r.assessmentCode()
                        + "'. Use ASG1, CT1, CAT1, ASG2, CT2, CAT2 or SEM"));
        if (r.rawMarks() == null) {
            throw new ValidationException("rawMarks is required");
        }
        if (!skipJavaValidation) {
            MarkValidator.validate(assessment.code(), r.rawMarks(), assessment.maxMarks());   // throws InvalidMarksException
        }
        try {
            repository.setClientIdentifier(username);        // same connection as the update (one transaction)
            repository.upsertMark(r.studentId(), r.subjectId(), assessment.id(), r.rawMarks());
        } finally {
            repository.clearClientIdentifier();
        }
        return repository.findMarks(r.studentId(), r.subjectId());
    }

    /**
     * Shows the calculation of ONE subject for ONE student, computed by Java and by Oracle,
     * so the evaluator can see that both give the same numbers.
     */
    public BreakdownResponse breakdown(long studentId, long subjectId) {
        Student student = students.get(studentId);
        Subject subject = subjects.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
        Map<String, BigDecimal> raw = repository.rawMarksByCode(studentId, subjectId);
        List<String> missing = List.of("ASG1", "CT1", "CAT1", "ASG2", "CT2", "CAT2", "SEM").stream()
                .filter(code -> !raw.containsKey(code)).toList();
        if (!missing.isEmpty()) {
            return new BreakdownResponse(studentId, student.getName(), subject.code(), null, null, null, null,
                    null, false, false, "Marks missing: " + String.join(", ", missing));
        }
        MarkBreakdown calc = MarkCalculator.calculate(raw.get("ASG1"), raw.get("CT1"), raw.get("CAT1"),
                raw.get("ASG2"), raw.get("CT2"), raw.get("CAT2"), raw.get("SEM"));
        BigDecimal oracleInternal = repository.oracleInternal(studentId, subjectId);
        BigDecimal oracleSemester = repository.oracleSemester(studentId, subjectId);
        BigDecimal oracleFinal = oracleInternal.add(oracleSemester).setScale(2, RoundingMode.HALF_UP);
        String grade = grades.loadScale().lookup(calc.finalMark()).map(GradeRule::code).orElse("?");
        boolean match = oracleInternal.compareTo(calc.internal()) == 0
                && oracleSemester.compareTo(calc.semester()) == 0
                && oracleFinal.compareTo(calc.finalMark()) == 0;
        return new BreakdownResponse(studentId, student.getName(), subject.code(), calc,
                oracleInternal, oracleSemester, oracleFinal, grade, match, true,
                match ? "Java and Oracle agree" : "MISMATCH between Java and Oracle");
    }
}
