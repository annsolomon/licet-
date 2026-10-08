package com.college.portals.advisor;

import com.college.portals.common.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** All SQL of the class-advisor portal. An advisor works only on the class where he or she is the advisor. */
@Repository
@Profile("advisor")
public class AdvisorRepository {

    /** One cell of the day grid. */
    public record Cell(long studentId, String studentName, int period, Long recordId, String status) { }

    private static final String PRESENT = "SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END)";
    private static final String ABSENT = "SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END)";
    private static final Set<String> STATUSES = Set.of("PRESENT", "ABSENT", "OD", "MEDICAL", "OTHER");

    private final JdbcTemplate jdbc;

    public AdvisorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long classOf(long facultyId) {
        Long id = jdbc.queryForObject("SELECT MIN(class_id) FROM class WHERE advisor_faculty_id = ?", Long.class, facultyId);
        if (id == null) {
            throw ApiException.notFound("You are not the advisor of any class");
        }
        return id;
    }

    public Map<String, Object> classInfo(long classId) {
        return jdbc.queryForList("""
                SELECT c.class_id "id", c.class_name "name", d.dept_code "dept", c.semester "semester", c.academic_year "year",
                       (SELECT COUNT(*) FROM student s WHERE s.class_id = c.class_id) "students",
                       (SELECT TO_CHAR(MAX(se.session_date), 'YYYY-MM-DD') FROM attendance_session se WHERE se.class_id = c.class_id) "latestDate"
                  FROM class c JOIN department d ON d.dept_id = c.dept_id WHERE c.class_id = ?
                """, classId).get(0);
    }

    public List<Map<String, Object>> periods(long classId, String date) {
        return jdbc.queryForList("""
                SELECT ps.period_no "period", ps.start_time "startTime", ps.end_time "endTime",
                       sb.subject_code "code", sb.subject_name "subject", f.faculty_name "faculty",
                       CASE WHEN se.session_id IS NULL THEN 0 ELSE 1 END "held",
                       (SELECT COUNT(*) FROM attendance_record x WHERE x.session_id = se.session_id AND x.status = 'ABSENT') "absent"
                  FROM period_slot ps
                  LEFT JOIN attendance_session se ON se.class_id = ? AND se.period_no = ps.period_no AND se.session_date = TO_DATE(?, 'YYYY-MM-DD')
                  LEFT JOIN subject sb ON sb.subject_id = se.subject_id
                  LEFT JOIN faculty f ON f.faculty_id = se.faculty_id
                 ORDER BY ps.period_no
                """, classId, date);
    }

    public List<Cell> cells(long classId, String date) {
        return jdbc.query("""
                SELECT s.student_id sid, s.student_name nm, ps.period_no pn, r.record_id rid, r.status st
                  FROM student s CROSS JOIN period_slot ps
                  LEFT JOIN attendance_session se ON se.class_id = s.class_id AND se.period_no = ps.period_no
                                                 AND se.session_date = TO_DATE(?, 'YYYY-MM-DD')
                  LEFT JOIN attendance_record r ON r.session_id = se.session_id AND r.student_id = s.student_id
                 WHERE s.class_id = ?
                 ORDER BY s.student_id, ps.period_no
                """, (rs, i) -> {
            long rid = rs.getLong("rid");
            return new Cell(rs.getLong("sid"), rs.getString("nm"), rs.getInt("pn"), rs.wasNull() ? null : rid, rs.getString("st"));
        }, date, classId);
    }

    public List<Map<String, Object>> students(long classId, String date) {
        return jdbc.queryForList("""
                SELECT s.student_id "id", s.student_name "name", p.parent_name "parentName", p.phone "parentPhone",
                       COUNT(r.record_id) "total", NVL(""" + PRESENT + ", 0) \"present\", NVL(" + ABSENT + ", 0) \"absent\", "
                + "ROUND(100 * " + PRESENT + " / NULLIF(COUNT(r.record_id), 0), 1) \"percentage\", " + """
                       (SELECT COUNT(*) FROM attendance_record r2 JOIN attendance_session se2 ON se2.session_id = r2.session_id
                         WHERE r2.student_id = s.student_id AND r2.status = 'ABSENT' AND se2.session_date = TO_DATE(?, 'YYYY-MM-DD')) "absentToday"
                  FROM student s
                  LEFT JOIN parent p ON p.parent_id = s.parent_id
                  LEFT JOIN attendance_record r ON r.student_id = s.student_id
                 WHERE s.class_id = ?
                 GROUP BY s.student_id, s.student_name, p.parent_name, p.phone
                 ORDER BY s.student_id
                """, date, classId);
    }

    public List<Map<String, Object>> subjects(long classId) {
        return jdbc.queryForList("""
                SELECT sb.subject_code "code", sb.subject_name "name", f.faculty_name "faculty",
                       COUNT(DISTINCT se.session_id) "periodsHeld", COUNT(*) "records", """ + ABSENT + " \"absent\", "
                + "ROUND(100 * " + PRESENT + " / COUNT(*), 1) \"percentage\" " + """
                  FROM attendance_session se
                  JOIN attendance_record r ON r.session_id = se.session_id
                  JOIN subject sb ON sb.subject_id = se.subject_id
                  JOIN class_subject cs ON cs.class_id = se.class_id AND cs.subject_id = se.subject_id
                  JOIN faculty f ON f.faculty_id = cs.faculty_id
                 WHERE se.class_id = ?
                 GROUP BY sb.subject_code, sb.subject_name, f.faculty_name
                 ORDER BY sb.subject_code
                """, classId);
    }

    public List<Map<String, Object>> low(long classId, int threshold) {
        return jdbc.queryForList("""
                SELECT * FROM (
                  SELECT s.student_id "id", s.student_name "name", p.parent_name "parentName", p.phone "parentPhone",
                         COUNT(*) "total", """ + PRESENT + " \"present\", " + ABSENT + " \"absent\", "
                + "ROUND(100 * " + PRESENT + " / COUNT(*), 1) \"percentage\" " + """
                    FROM student s
                    LEFT JOIN parent p ON p.parent_id = s.parent_id
                    JOIN attendance_record r ON r.student_id = s.student_id
                   WHERE s.class_id = ?
                   GROUP BY s.student_id, s.student_name, p.parent_name, p.phone
                ) WHERE "percentage" < ? ORDER BY "percentage"
                """, classId, threshold);
    }

    /** Correct one cell. The Oracle trigger notifies parent and advisor when the new status is ABSENT. */
    public void setStatus(long classId, long recordId, String status) {
        String st = status == null ? "" : status.trim().toUpperCase();
        if (!STATUSES.contains(st)) {
            throw ApiException.badRequest("Status must be one of " + STATUSES);
        }
        int n = jdbc.update("""
                UPDATE attendance_record SET status = ?
                 WHERE record_id = ? AND student_id IN (SELECT student_id FROM student WHERE class_id = ?)
                """, st, recordId, classId);
        if (n == 0) {
            throw ApiException.notFound("That attendance record is not in your class");
        }
    }

    /**
     * Mark a whole period: everybody PRESENT except the listed students (ABSENT).
     * The subject and teacher come from the timetable; the attendance session is created when it does not exist.
     */
    @Transactional
    public Map<String, Object> markPeriod(long classId, String date, int period, List<Long> absentIds) {
        LocalDate day = LocalDate.parse(date);
        if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw ApiException.badRequest("There are no classes on " + day.getDayOfWeek());
        }
        if (period < 1 || period > 8) {
            throw ApiException.badRequest("Period must be 1 to 8");
        }
        List<Map<String, Object>> tt = jdbc.queryForList(
                "SELECT subject_id \"subjectId\", faculty_id \"facultyId\" FROM timetable WHERE class_id = ? AND day_of_week = ? AND period_no = ?",
                classId, day.getDayOfWeek().getValue(), period);
        if (tt.isEmpty()) {
            throw ApiException.notFound("The timetable has no subject for that period");
        }
        long subjectId = ((Number) tt.get(0).get("subjectId")).longValue();
        long facultyId = ((Number) tt.get(0).get("facultyId")).longValue();

        List<Long> found = jdbc.queryForList("""
                SELECT session_id FROM attendance_session
                 WHERE class_id = ? AND subject_id = ? AND session_date = TO_DATE(?, 'YYYY-MM-DD') AND period_no = ?
                """, Long.class, classId, subjectId, date, period);
        long sessionId;
        if (found.isEmpty()) {
            sessionId = jdbc.queryForObject("SELECT seq_att_session.NEXTVAL FROM dual", Long.class);
            jdbc.update("INSERT INTO attendance_session (session_id, class_id, subject_id, faculty_id, session_date, period_no) "
                    + "VALUES (?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?)", sessionId, classId, subjectId, facultyId, date, period);
        } else {
            sessionId = found.get(0);
        }

        List<Long> students = jdbc.queryForList("SELECT student_id FROM student WHERE class_id = ? ORDER BY student_id", Long.class, classId);
        Set<Long> absent = Set.copyOf(absentIds == null ? List.of() : absentIds);
        for (Long id : absent) {
            if (!students.contains(id)) {
                throw ApiException.badRequest("Student " + id + " is not in your class");
            }
        }
        List<Object[]> batch = new ArrayList<>();
        for (Long id : students) {
            batch.add(new Object[]{sessionId, id, absent.contains(id) ? "ABSENT" : "PRESENT"});
        }
        jdbc.batchUpdate("""
                MERGE INTO attendance_record r
                USING (SELECT ? AS sid, ? AS stu, ? AS st FROM dual) s
                   ON (r.session_id = s.sid AND r.student_id = s.stu)
                 WHEN MATCHED THEN UPDATE SET r.status = s.st
                 WHEN NOT MATCHED THEN INSERT (record_id, session_id, student_id, status)
                      VALUES (seq_att_record.NEXTVAL, s.sid, s.stu, s.st)
                """, batch);
        return Map.of("sessionId", sessionId, "date", date, "period", period, "present", students.size() - absent.size(), "absent", absent.size());
    }
}
