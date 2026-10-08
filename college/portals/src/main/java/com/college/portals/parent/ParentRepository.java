package com.college.portals.parent;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** All SQL of the parent portal. Present = status PRESENT (same rule as the main application). */
@Repository
@Profile({"parent", "student"})
public class ParentRepository {

    private static final String PRESENT = "SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END)";
    private static final String ABSENT = "SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END)";

    private final JdbcTemplate jdbc;

    public ParentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> wards(String role, long linkId) {
        String by = "STUDENT".equals(role) ? "s.student_id" : "s.parent_id";
        return jdbc.queryForList("""
                SELECT s.student_id "id", s.student_name "name", s.email "email", s.phone "phone",
                       d.dept_code "dept", d.dept_name "deptName", c.class_name "className", c.semester "semester",
                       fa.faculty_name "advisorName", fa.email "advisorEmail", p.parent_name "parentName"
                  FROM student s
                  JOIN department d ON d.dept_id = s.dept_id
                  JOIN class c ON c.class_id = s.class_id
                  LEFT JOIN faculty fa ON fa.faculty_id = c.advisor_faculty_id
                  LEFT JOIN parent p ON p.parent_id = s.parent_id
                 WHERE """ + " " + by + " = ? ORDER BY s.student_id", linkId);
    }

    public boolean isWardOf(long parentId, long studentId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM student WHERE student_id = ? AND parent_id = ?",
                Integer.class, studentId, parentId);
        return n != null && n > 0;
    }

    public Map<String, Object> overall(long studentId) {
        return jdbc.queryForList("SELECT COUNT(*) \"total\", NVL(" + PRESENT + ", 0) \"present\", NVL(" + ABSENT + ", 0) \"absent\", "
                + "ROUND(100 * " + PRESENT + " / NULLIF(COUNT(*), 0), 1) \"percentage\" "
                + "FROM attendance_record r WHERE r.student_id = ?", studentId).get(0);
    }

    public List<Map<String, Object>> subjects(long studentId) {
        return jdbc.queryForList("""
                SELECT sb.subject_code "code", sb.subject_name "name", f.faculty_name "faculty", COUNT(*) "total",
                       """ + PRESENT + " \"present\", " + ABSENT + " \"absent\", ROUND(100 * " + PRESENT + " / COUNT(*), 1) \"percentage\" " + """
                  FROM attendance_record r
                  JOIN attendance_session se ON se.session_id = r.session_id
                  JOIN subject sb ON sb.subject_id = se.subject_id
                  JOIN class_subject cs ON cs.class_id = se.class_id AND cs.subject_id = se.subject_id
                  JOIN faculty f ON f.faculty_id = cs.faculty_id
                 WHERE r.student_id = ?
                 GROUP BY sb.subject_code, sb.subject_name, f.faculty_name
                 ORDER BY sb.subject_code
                """, studentId);
    }

    public String latestDate(long studentId) {
        return jdbc.queryForObject("""
                SELECT TO_CHAR(MAX(se.session_date), 'YYYY-MM-DD') FROM attendance_session se
                  JOIN student s ON s.class_id = se.class_id WHERE s.student_id = ?
                """, String.class, studentId);
    }

    /** The 8 periods of one day with the status of the ward (status is null when the period was not marked). */
    public List<Map<String, Object>> day(long studentId, String date) {
        return jdbc.queryForList("""
                SELECT ps.period_no "period", ps.start_time "startTime", ps.end_time "endTime",
                       sb.subject_code "code", sb.subject_name "subject", f.faculty_name "faculty", r.status "status"
                  FROM period_slot ps
                  JOIN student s ON s.student_id = ?
                  LEFT JOIN attendance_session se ON se.class_id = s.class_id AND se.period_no = ps.period_no
                                                 AND se.session_date = TO_DATE(?, 'YYYY-MM-DD')
                  LEFT JOIN subject sb ON sb.subject_id = se.subject_id
                  LEFT JOIN faculty f ON f.faculty_id = se.faculty_id
                  LEFT JOIN attendance_record r ON r.session_id = se.session_id AND r.student_id = s.student_id
                 ORDER BY ps.period_no
                """, studentId, date);
    }

    public List<Map<String, Object>> history(long studentId, int days) {
        return jdbc.queryForList("""
                SELECT TO_CHAR(se.session_date, 'YYYY-MM-DD') "date", TO_CHAR(se.session_date, 'Dy') "weekday",
                       COUNT(*) "periods", """ + PRESENT + " \"present\", " + ABSENT + " \"absent\", " + """
                       LISTAGG(CASE WHEN r.status = 'ABSENT' THEN TO_CHAR(se.period_no) END, ', ')
                         WITHIN GROUP (ORDER BY se.period_no) "absentPeriods"
                  FROM attendance_record r
                  JOIN attendance_session se ON se.session_id = r.session_id
                 WHERE r.student_id = ?
                   AND se.session_date > (SELECT MAX(session_date) FROM attendance_session) - ?
                 GROUP BY se.session_date
                 ORDER BY se.session_date DESC
                """, studentId, days);
    }
}
