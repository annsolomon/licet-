package com.college.portals.hod;

import com.college.portals.common.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** All SQL of the HOD portal. A HOD sees only his or her own department. */
@Repository
@Profile("hod")
public class HodRepository {

    private static final String PRESENT = "SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END)";
    private static final String ABSENT = "SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END)";
    private static final String PCT = "ROUND(100 * " + PRESENT + " / NULLIF(COUNT(*), 0), 1)";

    private final JdbcTemplate jdbc;

    public HodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long deptOf(long facultyId) {
        Long id = jdbc.queryForObject("SELECT MIN(dept_id) FROM department WHERE hod_faculty_id = ?", Long.class, facultyId);
        if (id == null) {
            throw ApiException.notFound("You are not the head of any department");
        }
        return id;
    }

    public Map<String, Object> department(long deptId) {
        return jdbc.queryForList("""
                SELECT d.dept_id "id", d.dept_code "code", d.dept_name "name",
                       (SELECT COUNT(*) FROM student s WHERE s.dept_id = d.dept_id) "students",
                       (SELECT COUNT(*) FROM faculty f WHERE f.dept_id = d.dept_id) "faculty",
                       (SELECT COUNT(*) FROM class c WHERE c.dept_id = d.dept_id) "classes",
                       (SELECT COUNT(*) FROM subject sb WHERE sb.dept_id = d.dept_id) "subjects",
                       (SELECT TO_CHAR(MAX(se.session_date), 'YYYY-MM-DD') FROM attendance_session se
                          JOIN class c ON c.class_id = se.class_id WHERE c.dept_id = d.dept_id) "latestDate"
                  FROM department d WHERE d.dept_id = ?
                """, deptId).get(0);
    }

    public Map<String, Object> dayStats(long deptId, String date) {
        return jdbc.queryForList("SELECT COUNT(*) \"records\", NVL(" + PRESENT + ", 0) \"present\", NVL(" + ABSENT + ", 0) \"absent\", "
                + PCT + " \"percentage\", COUNT(DISTINCT CASE WHEN r.status = 'ABSENT' THEN r.student_id END) \"studentsAbsent\" " + """
                  FROM attendance_record r
                  JOIN attendance_session se ON se.session_id = r.session_id
                  JOIN class c ON c.class_id = se.class_id
                 WHERE c.dept_id = ? AND se.session_date = TO_DATE(?, 'YYYY-MM-DD')
                """, deptId, date).get(0);
    }

    public List<Map<String, Object>> periodWise(long deptId, String date) {
        return jdbc.queryForList("""
                SELECT ps.period_no "period", ps.start_time "startTime", """ + PCT + " \"percentage\", NVL(" + ABSENT + ", 0) \"absent\", COUNT(r.record_id) \"records\" " + """
                  FROM period_slot ps
                  LEFT JOIN attendance_session se ON se.period_no = ps.period_no AND se.session_date = TO_DATE(?, 'YYYY-MM-DD')
                       AND se.class_id IN (SELECT class_id FROM class WHERE dept_id = ?)
                  LEFT JOIN attendance_record r ON r.session_id = se.session_id
                 GROUP BY ps.period_no, ps.start_time
                 ORDER BY ps.period_no
                """, date, deptId);
    }

    public List<Map<String, Object>> trend(long deptId, int days) {
        return jdbc.queryForList("""
                SELECT TO_CHAR(se.session_date, 'YYYY-MM-DD') "date", TO_CHAR(se.session_date, 'Dy') "weekday", """ + PCT + " \"percentage\", NVL(" + ABSENT + ", 0) \"absent\" " + """
                  FROM attendance_record r
                  JOIN attendance_session se ON se.session_id = r.session_id
                  JOIN class c ON c.class_id = se.class_id
                 WHERE c.dept_id = ? AND se.session_date > (SELECT MAX(session_date) FROM attendance_session) - ?
                 GROUP BY se.session_date
                 ORDER BY se.session_date
                """, deptId, days);
    }

    public List<Map<String, Object>> classes(long deptId) {
        return jdbc.queryForList("""
                SELECT c.class_id "id", c.class_name "name", fa.faculty_name "advisor", fa.email "advisorEmail",
                       (SELECT COUNT(*) FROM student s WHERE s.class_id = c.class_id) "students",
                       (SELECT ROUND(100 * SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*), 1)
                          FROM attendance_record r JOIN attendance_session se ON se.session_id = r.session_id
                         WHERE se.class_id = c.class_id) "percentage"
                  FROM class c LEFT JOIN faculty fa ON fa.faculty_id = c.advisor_faculty_id
                 WHERE c.dept_id = ? ORDER BY c.class_id
                """, deptId);
    }

    public List<Map<String, Object>> subjects(long deptId) {
        return jdbc.queryForList("""
                SELECT c.class_name "className", sb.subject_code "code", sb.subject_name "name", f.faculty_name "faculty",
                       COUNT(DISTINCT se.session_id) "periodsHeld", COUNT(*) "records", NVL(""" + ABSENT + ", 0) \"absent\", " + PCT + " \"percentage\" " + """
                  FROM attendance_session se
                  JOIN attendance_record r ON r.session_id = se.session_id
                  JOIN class c ON c.class_id = se.class_id
                  JOIN subject sb ON sb.subject_id = se.subject_id
                  JOIN class_subject cs ON cs.class_id = se.class_id AND cs.subject_id = se.subject_id
                  JOIN faculty f ON f.faculty_id = cs.faculty_id
                 WHERE c.dept_id = ?
                 GROUP BY c.class_name, sb.subject_code, sb.subject_name, f.faculty_name
                 ORDER BY sb.subject_code
                """, deptId);
    }

    public List<Map<String, Object>> faculty(long deptId) {
        return jdbc.queryForList("""
                SELECT f.faculty_id "id", f.faculty_name "name", f.designation "designation", f.email "email",
                       (SELECT LISTAGG(sb.subject_code, ', ') WITHIN GROUP (ORDER BY sb.subject_code)
                          FROM class_subject cs JOIN subject sb ON sb.subject_id = cs.subject_id
                         WHERE cs.faculty_id = f.faculty_id) "subjects",
                       (SELECT MIN(c.class_name) FROM class c WHERE c.advisor_faculty_id = f.faculty_id) "advisorOf",
                       (SELECT COUNT(*) FROM attendance_session se WHERE se.faculty_id = f.faculty_id) "periodsTaken",
                       (SELECT ROUND(100 * SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 1)
                          FROM attendance_session se JOIN attendance_record r ON r.session_id = se.session_id
                         WHERE se.faculty_id = f.faculty_id) "classAttendance"
                  FROM faculty f WHERE f.dept_id = ? ORDER BY f.faculty_id
                """, deptId);
    }

    public List<Map<String, Object>> low(long deptId, int threshold) {
        return jdbc.queryForList("""
                SELECT * FROM (
                  SELECT s.student_id "id", s.student_name "name", c.class_name "className",
                         p.parent_name "parentName", p.phone "parentPhone",
                         COUNT(*) "total", """ + PRESENT + " \"present\", " + ABSENT + " \"absent\", " + PCT + " \"percentage\" " + """
                    FROM student s
                    JOIN class c ON c.class_id = s.class_id
                    LEFT JOIN parent p ON p.parent_id = s.parent_id
                    JOIN attendance_record r ON r.student_id = s.student_id
                   WHERE s.dept_id = ?
                   GROUP BY s.student_id, s.student_name, c.class_name, p.parent_name, p.phone
                ) WHERE "percentage" < ? ORDER BY "percentage"
                """, deptId, threshold);
    }

    public List<Map<String, Object>> absentees(long deptId, String date) {
        return jdbc.queryForList("""
                SELECT s.student_id "id", s.student_name "name", c.class_name "className", COUNT(*) "absentPeriods",
                       LISTAGG(TO_CHAR(se.period_no), ', ') WITHIN GROUP (ORDER BY se.period_no) "periods",
                       MAX(p.phone) "parentPhone"
                  FROM attendance_record r
                  JOIN attendance_session se ON se.session_id = r.session_id
                  JOIN student s ON s.student_id = r.student_id
                  JOIN class c ON c.class_id = s.class_id
                  LEFT JOIN parent p ON p.parent_id = s.parent_id
                 WHERE s.dept_id = ? AND r.status = 'ABSENT' AND se.session_date = TO_DATE(?, 'YYYY-MM-DD')
                 GROUP BY s.student_id, s.student_name, c.class_name
                 ORDER BY COUNT(*) DESC, s.student_id
                 FETCH FIRST 150 ROWS ONLY
                """, deptId, date);
    }

    public List<Map<String, Object>> notificationSummary(long deptId) {
        return jdbc.queryForList("""
                SELECT n.recipient_type "recipient", n.status "status", COUNT(*) "count"
                  FROM notification n
                  LEFT JOIN student s ON s.student_id = n.student_id
                  LEFT JOIN attendance_session se ON se.session_id = n.session_id
                  LEFT JOIN class c ON c.class_id = se.class_id
                 WHERE NVL(s.dept_id, c.dept_id) = ?
                 GROUP BY n.recipient_type, n.status ORDER BY n.recipient_type, n.status
                """, deptId);
    }
}
