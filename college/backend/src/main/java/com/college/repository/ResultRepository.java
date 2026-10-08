package com.college.repository;

import com.college.dto.SemesterGpa;
import com.college.model.ResultRow;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RESULT table + the Oracle stored procedures / functions that fill and summarise it.
 *
 *   React -> ResultController -> ResultService -> ResultRepository
 *         -> JdbcTemplate -> CALL calculate_student_result(?, ?) -> Oracle -> RESULT row -> JSON
 */
@Repository
public class ResultRepository {

    private static final String SELECT = """
            SELECT r.result_id, r.student_id, s.student_name, d.dept_code,
                   r.subject_id, sb.subject_code, sb.subject_name, r.semester,
                   r.internal_mark, r.semester_mark, r.final_mark,
                   r.grade_code, r.grade_point, r.credits, r.pass_fail
              FROM result r
              JOIN student    s  ON s.student_id  = r.student_id
              JOIN department d  ON d.dept_id     = s.dept_id
              JOIN subject    sb ON sb.subject_id = r.subject_id
            """;

    private static final RowMapper<ResultRow> MAPPER = (rs, i) -> new ResultRow(
            rs.getLong("result_id"), rs.getLong("student_id"), rs.getString("student_name"), rs.getString("dept_code"),
            rs.getLong("subject_id"), rs.getString("subject_code"), rs.getString("subject_name"), rs.getInt("semester"),
            rs.getBigDecimal("internal_mark"), rs.getBigDecimal("semester_mark"), rs.getBigDecimal("final_mark"),
            rs.getString("grade_code"), rs.getBigDecimal("grade_point"), rs.getInt("credits"), rs.getString("pass_fail"));

    private final JdbcTemplate jdbc;

    public ResultRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ stored procedures
    /**
     * CALLS the PL/SQL procedure. Oracle reads the 7 raw marks, applies the formula (functions from 06),
     * looks up the grade, and inserts/updates the RESULT row with MERGE.
     * An anonymous block "BEGIN ... END;" is the simplest way to call a procedure from JdbcTemplate.
     */
    public void callCalculateStudentResult(long studentId, long subjectId) {
        jdbc.update("BEGIN calculate_student_result(?, ?); END;", studentId, subjectId);
    }

    /** Calls a procedure that has an OUT parameter, using plain JDBC CallableStatement inside JdbcTemplate. */
    public int callCalculateAllResults() {
        Integer count = jdbc.execute((ConnectionCallback<Integer>) con -> {
            try (CallableStatement cs = con.prepareCall("{call calculate_all_results(?)}")) {
                cs.registerOutParameter(1, Types.NUMERIC);   // OUT parameter: how many results were calculated
                cs.execute();
                return cs.getInt(1);
            }
        });
        return count == null ? 0 : count;
    }

    // ------------------------------------------------------------------ queries
    public List<ResultRow> findAll() {
        return jdbc.query(SELECT + " ORDER BY r.student_id, r.semester, sb.subject_code", MAPPER);
    }

    public List<ResultRow> findBySemester(int semester) {
        return jdbc.query(SELECT + " WHERE r.semester = ? ORDER BY r.student_id, sb.subject_code", MAPPER, semester);
    }

    public List<ResultRow> findByStudent(long studentId) {
        return jdbc.query(SELECT + " WHERE r.student_id = ? ORDER BY r.semester, sb.subject_code", MAPPER, studentId);
    }

    public List<ResultRow> findByDepartment(long deptId) {
        return jdbc.query(SELECT + " WHERE s.dept_id = ? ORDER BY r.student_id, r.semester, sb.subject_code", MAPPER, deptId);
    }

    /** SGPA per semester using the Oracle function calculate_sgpa(). */
    public List<SemesterGpa> sgpaBySemester(long studentId) {
        return jdbc.query("""
                SELECT semester, calculate_sgpa(student_id, semester) AS sgpa, SUM(credits) AS credits
                  FROM result
                 WHERE student_id = ?
                 GROUP BY student_id, semester
                 ORDER BY semester
                """, (rs, i) -> new SemesterGpa(rs.getInt("semester"), rs.getBigDecimal("sgpa"), rs.getInt("credits")),
                studentId);
    }

    /** CGPA using the Oracle function calculate_cgpa(). */
    public BigDecimal oracleCgpa(long studentId) {
        return jdbc.queryForObject("SELECT calculate_cgpa(?) FROM dual", BigDecimal.class, studentId);
    }

    public List<Map<String, Object>> topPerformers(int limit) {
        return jdbc.queryForList("""
                SELECT student_id AS "studentId", student_name AS "studentName", dept_code AS "deptCode",
                       cgpa AS "cgpa", overall_attendance_pct AS "attendancePct"
                  FROM student_cgpa_view
                 WHERE cgpa IS NOT NULL
                 ORDER BY cgpa DESC, student_id
                 FETCH FIRST ? ROWS ONLY
                """, limit);
    }

    public Map<String, Long> gradeDistribution() {
        Map<String, Long> map = new LinkedHashMap<>();
        jdbc.query("""
                SELECT g.grade_code, COUNT(r.result_id) AS n
                  FROM grade g LEFT JOIN result r ON r.grade_code = g.grade_code
                 GROUP BY g.grade_code, g.grade_point
                 ORDER BY g.grade_point DESC
                """, rs -> {
            map.put(rs.getString("grade_code"), rs.getLong("n"));
        });
        return map;
    }

    public long countByPassFail(String passFail) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM result WHERE pass_fail = ?", Long.class, passFail);
        return n == null ? 0 : n;
    }
}
