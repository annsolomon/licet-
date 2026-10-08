package com.college.repository;

import com.college.model.Assessment;
import com.college.model.MarkEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SQL for ASSESSMENT (definitions) and MARKS (RAW marks only - converted marks are never stored here).
 *
 *   React -> MarksController -> MarksService -> MarksRepository -> Oracle MARKS
 *         -> (trigger audits + validates) -> calculation -> RESULT
 */
@Repository
public class MarksRepository {

    private static final RowMapper<Assessment> ASSESSMENT_MAPPER = (rs, i) -> new Assessment(
            rs.getLong("assessment_id"), rs.getString("assessment_code"), rs.getString("assessment_name"),
            rs.getBigDecimal("max_marks"), rs.getInt("part_no"));

    private static final String MARK_SELECT = """
            SELECT m.marks_id, m.student_id, m.subject_id, sb.subject_code,
                   a.assessment_code, a.assessment_name, m.raw_marks, a.max_marks
              FROM marks m
              JOIN assessment a  ON a.assessment_id = m.assessment_id
              JOIN subject    sb ON sb.subject_id   = m.subject_id
            """;

    private static final RowMapper<MarkEntry> MARK_MAPPER = (rs, i) -> new MarkEntry(
            rs.getLong("marks_id"), rs.getLong("student_id"), rs.getLong("subject_id"), rs.getString("subject_code"),
            rs.getString("assessment_code"), rs.getString("assessment_name"),
            rs.getBigDecimal("raw_marks"), rs.getBigDecimal("max_marks"));

    private final JdbcTemplate jdbc;

    public MarksRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Assessment> findAssessments() {
        return jdbc.query("SELECT assessment_id, assessment_code, assessment_name, max_marks, part_no FROM assessment ORDER BY assessment_id",
                ASSESSMENT_MAPPER);
    }

    public Optional<Assessment> findAssessmentByCode(String code) {
        return jdbc.query("SELECT assessment_id, assessment_code, assessment_name, max_marks, part_no FROM assessment WHERE assessment_code = ?",
                ASSESSMENT_MAPPER, code).stream().findFirst();
    }

    /** Raw marks of a student (optionally for one subject only: pass null). */
    public List<MarkEntry> findMarks(long studentId, Long subjectId) {
        if (subjectId == null) {
            return jdbc.query(MARK_SELECT + " WHERE m.student_id = ? ORDER BY sb.subject_code, a.assessment_id",
                    MARK_MAPPER, studentId);
        }
        return jdbc.query(MARK_SELECT + " WHERE m.student_id = ? AND m.subject_id = ? ORDER BY a.assessment_id",
                MARK_MAPPER, studentId, subjectId);
    }

    /** assessment code -> raw mark, in assessment order (ASG1, CT1, CAT1, ASG2, CT2, CAT2, SEM). */
    public Map<String, BigDecimal> rawMarksByCode(long studentId, long subjectId) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (MarkEntry e : findMarks(studentId, subjectId)) {
            map.put(e.assessmentCode(), e.rawMarks());
        }
        return map;
    }

    /**
     * Insert-or-update ONE raw mark. If a row exists the UPDATE fires the Oracle triggers:
     *   trg_marks_validate (rejects marks > max) and trg_marks_audit_update (writes AUDIT_LOG).
     */
    public int upsertMark(long studentId, long subjectId, long assessmentId, BigDecimal raw) {
        return jdbc.update("""
                MERGE INTO marks m
                USING (SELECT ? AS stid, ? AS subid, ? AS asid FROM dual) src
                   ON (m.student_id = src.stid AND m.subject_id = src.subid AND m.assessment_id = src.asid)
                WHEN MATCHED THEN UPDATE SET m.raw_marks = ?
                WHEN NOT MATCHED THEN INSERT (marks_id, student_id, subject_id, assessment_id, raw_marks)
                     VALUES (seq_marks.NEXTVAL, src.stid, src.subid, src.asid, ?)
                """, studentId, subjectId, assessmentId, raw, raw);
    }

    /**
     * Tells Oracle WHO is making the change (read by the audit trigger through SYS_CONTEXT).
     * Must run on the SAME connection as the update, i.e. inside the service's transaction.
     */
    public void setClientIdentifier(String username) {
        jdbc.update("BEGIN DBMS_SESSION.SET_IDENTIFIER(?); END;", username);
    }

    public void clearClientIdentifier() {
        jdbc.update("BEGIN DBMS_SESSION.CLEAR_IDENTIFIER; END;");
    }

    // ---- the same calculation done by Oracle functions (06_functions.sql) ----
    public BigDecimal oracleInternal(long studentId, long subjectId) {
        return jdbc.queryForObject("SELECT calculate_internal_mark(?, ?) FROM dual", BigDecimal.class, studentId, subjectId);
    }

    public BigDecimal oracleSemester(long studentId, long subjectId) {
        return jdbc.queryForObject("SELECT calculate_semester_mark(?, ?) FROM dual", BigDecimal.class, studentId, subjectId);
    }

    public String oracleGrade(BigDecimal finalMark) {
        return jdbc.queryForObject("SELECT calculate_grade(?) FROM dual", String.class, finalMark);
    }

    public boolean subjectExists(long subjectId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM subject WHERE subject_id = ?", Integer.class, subjectId);
        return n != null && n > 0;
    }
}
