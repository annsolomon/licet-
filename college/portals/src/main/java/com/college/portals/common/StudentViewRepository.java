package com.college.portals.common;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Queries shared by the portals that show a student's attendance and internal-assessment marks
 * (HOD, advisor, parent and student).
 * Internal mark /40 = (part1 + part2) / 5, part = ASG + (CT + CAT) * 2/3 - the same formula as the Oracle function
 * CALCULATE_INTERNAL_MARK, written as set-based SQL here so a whole department is one query.
 */
@Repository
@Profile({"hod", "advisor", "parent", "student"})
public class StudentViewRepository {

    public static final int PASS_MARK = 20;     // 50 % of 40

    private static final String IM = """
            (SELECT m.student_id, m.subject_id,
                    MAX(CASE a.assessment_code WHEN 'ASG1' THEN m.raw_marks END) asg1,
                    MAX(CASE a.assessment_code WHEN 'CT1'  THEN m.raw_marks END) ct1,
                    MAX(CASE a.assessment_code WHEN 'CAT1' THEN m.raw_marks END) cat1,
                    MAX(CASE a.assessment_code WHEN 'ASG2' THEN m.raw_marks END) asg2,
                    MAX(CASE a.assessment_code WHEN 'CT2'  THEN m.raw_marks END) ct2,
                    MAX(CASE a.assessment_code WHEN 'CAT2' THEN m.raw_marks END) cat2
               FROM marks m JOIN assessment a ON a.assessment_id = m.assessment_id
              GROUP BY m.student_id, m.subject_id) im""";
    private static final String INTERNAL = "ROUND(((im.asg1 + (im.ct1 + im.cat1) * 2 / 3) + (im.asg2 + (im.ct2 + im.cat2) * 2 / 3)) / 5, 2)";

    private final JdbcTemplate jdbc;

    private static String sql(String text) {
        return text.replace("{IM}", IM).replace("{INT}", INTERNAL).replace("{PASS}", String.valueOf(PASS_MARK));
    }

    public StudentViewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** department and class of a student, to check that the HOD / advisor may look at him or her */
    public Map<String, Object> scope(long studentId) {
        List<Map<String, Object>> r = jdbc.queryForList(
                "SELECT dept_id \"deptId\", class_id \"classId\" FROM student WHERE student_id = ?", studentId);
        if (r.isEmpty()) {
            throw ApiException.notFound("No such student");
        }
        return r.get(0);
    }

    public Map<String, Object> profile(long studentId) {
        List<Map<String, Object>> r = jdbc.queryForList("""
                SELECT s.student_id "id", s.student_name "name", s.email "email", s.phone "phone", d.dept_name "deptName",
                       c.class_name "className", fa.faculty_name "advisorName", p.parent_name "parentName", p.phone "parentPhone"
                  FROM student s
                  JOIN department d ON d.dept_id = s.dept_id
                  JOIN class c ON c.class_id = s.class_id
                  LEFT JOIN faculty fa ON fa.faculty_id = c.advisor_faculty_id
                  LEFT JOIN parent p ON p.parent_id = s.parent_id
                 WHERE s.student_id = ?
                """, studentId);
        if (r.isEmpty()) {
            throw ApiException.notFound("No such student");
        }
        return r.get(0);
    }

    public Map<String, Object> attendance(long studentId) {
        return jdbc.queryForList("""
                SELECT COUNT(*) "total", NVL(SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END), 0) "present",
                       NVL(SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END), 0) "absent",
                       ROUND(100 * SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 1) "percentage"
                  FROM attendance_record WHERE student_id = ?
                """, studentId).get(0);
    }

    public List<Map<String, Object>> subjectAttendance(long studentId) {
        return jdbc.queryForList("""
                SELECT sb.subject_code "code", sb.subject_name "name", f.faculty_name "faculty", COUNT(*) "total",
                       SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END) "present",
                       SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END) "absent",
                       ROUND(100 * SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*), 1) "percentage"
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

    /** All internal marks of one student, subject by subject, with the /40 result. */
    public Map<String, Object> internals(long studentId) {
        List<Map<String, Object>> subjects = jdbc.queryForList(sql("""
                SELECT sb.subject_code "code", sb.subject_name "name", f.faculty_name "faculty",
                       im.asg1 "asg1", im.ct1 "ct1", im.cat1 "cat1", im.asg2 "asg2", im.ct2 "ct2", im.cat2 "cat2",
                       {INT}
                        "internal"
                  FROM student s
                  JOIN class_subject cs ON cs.class_id = s.class_id
                  JOIN subject sb ON sb.subject_id = cs.subject_id
                  JOIN faculty f ON f.faculty_id = cs.faculty_id
                  LEFT JOIN {IM}
                    ON im.student_id = s.student_id AND im.subject_id = sb.subject_id
                 WHERE s.student_id = ?
                 ORDER BY sb.subject_code
                """), studentId);
        double sum = 0;
        int n = 0;
        int below = 0;
        for (Map<String, Object> row : subjects) {
            Object v = row.get("internal");
            if (v != null) {
                double d = ((Number) v).doubleValue();
                sum += d;
                n++;
                if (d < PASS_MARK) {
                    below++;
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("passMark", PASS_MARK);
        out.put("maxMarks", jdbc.queryForList("SELECT LOWER(assessment_code) \"code\", max_marks \"max\" FROM assessment WHERE part_no < 3 ORDER BY assessment_id"));
        out.put("subjects", subjects);
        out.put("average", n == 0 ? null : Math.round(sum / n * 10) / 10.0);
        out.put("below", below);
        return out;
    }

    /** every student of a department (scope = "s.dept_id") or class (scope = "s.class_id") with attendance and internals */
    public List<Map<String, Object>> overview(String scope, long id) {
        return jdbc.queryForList(sql("""
                SELECT s.student_id "id", s.student_name "name", c.class_name "className",
                       ROUND(100 * att.pres / NULLIF(att.tot, 0), 1) "attendance",
                       att.absent "absent",
                       ROUND(itn.avg_i, 1) "internalAvg", itn.below "below",
                       p.parent_name "parentName", p.phone "parentPhone"
                  FROM student s
                  JOIN class c ON c.class_id = s.class_id
                  LEFT JOIN parent p ON p.parent_id = s.parent_id
                  LEFT JOIN (SELECT student_id, COUNT(*) tot, SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) pres,
                                    SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) absent
                               FROM attendance_record GROUP BY student_id) att ON att.student_id = s.student_id
                  LEFT JOIN (SELECT im.student_id, AVG({INT}
                        ) avg_i,
                                    SUM(CASE WHEN {INT} < {PASS}
                         THEN 1 ELSE 0 END) below
                               FROM {IM}
                               JOIN student s2 ON s2.student_id = im.student_id
                               JOIN class_subject cs ON cs.class_id = s2.class_id AND cs.subject_id = im.subject_id
                              GROUP BY im.student_id) itn ON itn.student_id = s.student_id
                 WHERE {SCOPE}
                 = ?
                 ORDER BY s.student_id
                """.replace("{SCOPE}", scope)), id);
    }

    /** class x subject summary of the internal marks */
    public List<Map<String, Object>> subjectSummary(String scope, long id) {
        return jdbc.queryForList(sql("""
                SELECT c.class_name "className", sb.subject_code "code", sb.subject_name "name", f.faculty_name "faculty",
                       COUNT({INT}
                       ) "students",
                       ROUND(AVG({INT}
                       ), 1) "average",
                       MIN({INT}
                       ) "lowest",
                       MAX({INT}
                       ) "highest",
                       SUM(CASE WHEN {INT} < {PASS}
                        THEN 1 ELSE 0 END) "below"
                  FROM student s
                  JOIN class c ON c.class_id = s.class_id
                  JOIN class_subject cs ON cs.class_id = s.class_id
                  JOIN subject sb ON sb.subject_id = cs.subject_id
                  JOIN faculty f ON f.faculty_id = cs.faculty_id
                  JOIN {IM}
                    ON im.student_id = s.student_id AND im.subject_id = sb.subject_id
                 WHERE {SCOPE}
                 = ?
                 GROUP BY c.class_name, sb.subject_code, sb.subject_name, f.faculty_name
                 ORDER BY c.class_name, sb.subject_code
                """.replace("{SCOPE}", scope)), id);
    }

    /** everything about one student: profile, attendance (overall and subject-wise) and internals */
    public Map<String, Object> detail(long studentId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("profile", profile(studentId));
        out.put("attendance", attendance(studentId));
        out.put("subjects", subjectAttendance(studentId));
        out.put("internals", internals(studentId));
        return out;
    }

    /** students x subjects grid of internal marks (/40), for the advisor's class */
    public List<Map<String, Object>> grid(long classId) {
        return jdbc.queryForList(sql("""
                SELECT s.student_id "id", s.student_name "name", sb.subject_code "code", {INT}
                        "internal"
                  FROM student s
                  JOIN class_subject cs ON cs.class_id = s.class_id
                  JOIN subject sb ON sb.subject_id = cs.subject_id
                  LEFT JOIN {IM}
                    ON im.student_id = s.student_id AND im.subject_id = sb.subject_id
                 WHERE s.class_id = ?
                 ORDER BY s.student_id, sb.subject_code
                """), classId);
    }
}
