package com.college.repository;

import com.college.dto.AttendanceLine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL for ATTENDANCE_SESSION (one class period) and ATTENDANCE_RECORD (one student in that period).
 *
 *   Faculty -> Attendance page -> REST -> Spring Boot -> JdbcTemplate -> Oracle ATTENDANCE_RECORD
 *           -> attendance % -> Dashboard
 */
@Repository
public class AttendanceRepository {

    /** Raw numbers of one group (one student overall / in one subject): the service turns them into %. */
    public record Counts(long studentId, String studentName, String subjectCode, long total, long present) { }

    private static final String COUNT_SQL_START = """
            SELECT s.student_id, s.student_name, %s AS subject_code,
                   COUNT(*) AS total,
                   SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END) AS present
              FROM attendance_record  ar
              JOIN attendance_session se ON se.session_id = ar.session_id
              JOIN student            s  ON s.student_id  = ar.student_id
              JOIN subject            sb ON sb.subject_id = se.subject_id
            """;

    private static final RowMapper<Counts> COUNT_MAPPER = (rs, i) -> new Counts(
            rs.getLong("student_id"), rs.getString("student_name"), rs.getString("subject_code"),
            rs.getLong("total"), rs.getLong("present"));

    private static final String LINES_SQL = """
            SELECT ar.record_id, ar.session_id, ar.student_id, s.student_name, sb.subject_code,
                   f.faculty_name, se.session_date, se.period_no, ar.status
              FROM attendance_record  ar
              JOIN attendance_session se ON se.session_id = ar.session_id
              JOIN student            s  ON s.student_id  = ar.student_id
              JOIN subject            sb ON sb.subject_id = se.subject_id
              JOIN faculty            f  ON f.faculty_id  = se.faculty_id
            """;

    private static final RowMapper<AttendanceLine> LINE_MAPPER = (rs, i) -> new AttendanceLine(
            rs.getLong("record_id"), rs.getLong("session_id"), rs.getLong("student_id"),
            rs.getString("student_name"), rs.getString("subject_code"), rs.getString("faculty_name"),
            RsUtil.dateText(rs, "session_date"), rs.getInt("period_no"), rs.getString("status"));

    private final JdbcTemplate jdbc;

    public AttendanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ sessions
    public Optional<Long> findSession(long classId, long subjectId, LocalDate date, int period) {
        List<Long> ids = jdbc.query("""
                SELECT session_id FROM attendance_session
                 WHERE class_id = ? AND subject_id = ? AND session_date = ? AND period_no = ?
                """, (rs, i) -> rs.getLong(1), classId, subjectId, RsUtil.sqlDate(date), period);
        return ids.stream().findFirst();
    }

    public long createSession(long classId, long subjectId, long facultyId, LocalDate date, int period) {
        Long id = jdbc.queryForObject("SELECT seq_att_session.NEXTVAL FROM dual", Long.class);
        long sessionId = id == null ? 0 : id;
        jdbc.update("""
                INSERT INTO attendance_session (session_id, class_id, subject_id, faculty_id, session_date, period_no)
                VALUES (?, ?, ?, ?, ?, ?)
                """, sessionId, classId, subjectId, facultyId, RsUtil.sqlDate(date), period);
        return sessionId;
    }

    // ------------------------------------------------------------------ records
    /**
     * Insert-or-update every record of one sheet in ONE batch (MERGE = "update if it exists, else insert").
     * Batch = the driver sends all rows together, much faster than one round trip per student.
     */
    public int[] upsertRecords(long sessionId, List<Long> studentIds, List<String> statuses) {
        List<Object[]> args = new ArrayList<>();
        for (int i = 0; i < studentIds.size(); i++) {
            String status = statuses.get(i);
            args.add(new Object[]{sessionId, studentIds.get(i), status, status});
        }
        return jdbc.batchUpdate("""
                MERGE INTO attendance_record ar
                USING (SELECT ? AS sid, ? AS stid FROM dual) src
                   ON (ar.session_id = src.sid AND ar.student_id = src.stid)
                WHEN MATCHED THEN UPDATE SET ar.status = ?
                WHEN NOT MATCHED THEN INSERT (record_id, session_id, student_id, status)
                     VALUES (seq_att_record.NEXTVAL, src.sid, src.stid, ?)
                """, args);
    }

    /** Calls the stored procedure mark_attendance(session, student, status) - the PL/SQL way. */
    public void callMarkAttendance(long sessionId, long studentId, String status) {
        jdbc.update("BEGIN mark_attendance(?, ?, ?); END;", sessionId, studentId, status);
    }

    public boolean studentInClass(long studentId, long classId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM student WHERE student_id = ? AND class_id = ?", Integer.class, studentId, classId);
        return n != null && n > 0;
    }

    // ------------------------------------------------------------------ views for the screens
    /** Per student, per subject (a student's own attendance). */
    public List<Counts> countsByStudent(long studentId) {
        return jdbc.query(String.format(COUNT_SQL_START, "sb.subject_code")
                + " WHERE s.student_id = ? GROUP BY s.student_id, s.student_name, sb.subject_code ORDER BY sb.subject_code",
                COUNT_MAPPER, studentId);
    }

    /** All subjects together for one student. */
    public Optional<Counts> overallForStudent(long studentId) {
        return jdbc.query(String.format(COUNT_SQL_START, "'ALL'")
                + " WHERE s.student_id = ? GROUP BY s.student_id, s.student_name", COUNT_MAPPER, studentId)
                .stream().findFirst();
    }

    /** Per student for ONE subject (subject attendance). */
    public List<Counts> countsBySubject(long subjectId) {
        return jdbc.query(String.format(COUNT_SQL_START, "sb.subject_code")
                + " WHERE sb.subject_id = ? GROUP BY s.student_id, s.student_name, sb.subject_code ORDER BY s.student_id",
                COUNT_MAPPER, subjectId);
    }

    /** Per student, all subjects, for one class. */
    public List<Counts> countsByClass(long classId) {
        return jdbc.query(String.format(COUNT_SQL_START, "'ALL'")
                + " WHERE s.class_id = ? GROUP BY s.student_id, s.student_name ORDER BY s.student_id",
                COUNT_MAPPER, classId);
    }

    /** Per student, all subjects, for one department. */
    public List<Counts> countsByDepartment(long deptId) {
        return jdbc.query(String.format(COUNT_SQL_START, "'ALL'")
                + " WHERE s.dept_id = ? GROUP BY s.student_id, s.student_name ORDER BY s.student_id",
                COUNT_MAPPER, deptId);
    }

    /** Every student (used by the low-attendance report and the dashboard). */
    public List<Counts> countsAll() {
        return jdbc.query(String.format(COUNT_SQL_START, "'ALL'")
                + " GROUP BY s.student_id, s.student_name ORDER BY s.student_id", COUNT_MAPPER);
    }

    /** Individual records of a class period list (newest first, max 300 rows). */
    public List<AttendanceLine> linesForStudent(long studentId) {
        return jdbc.query(LINES_SQL + " WHERE ar.student_id = ? ORDER BY se.session_date DESC, se.period_no FETCH FIRST 300 ROWS ONLY",
                LINE_MAPPER, studentId);
    }

    public List<AttendanceLine> linesForSession(long sessionId) {
        return jdbc.query(LINES_SQL + " WHERE ar.session_id = ? ORDER BY ar.student_id", LINE_MAPPER, sessionId);
    }

    /** Sessions held for a class + subject (to pick a sheet to view). */
    public List<java.util.Map<String, Object>> sessions(long classId, long subjectId) {
        return jdbc.queryForList("""
                SELECT se.session_id AS "sessionId", TO_CHAR(se.session_date, 'YYYY-MM-DD') AS "date",
                       se.period_no AS "period", f.faculty_name AS "faculty"
                  FROM attendance_session se JOIN faculty f ON f.faculty_id = se.faculty_id
                 WHERE se.class_id = ? AND se.subject_id = ?
                 ORDER BY se.session_date DESC, se.period_no
                """, classId, subjectId);
    }
}
