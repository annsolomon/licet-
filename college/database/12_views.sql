-- =====================================================================
-- 12_views.sql                                       SYLLABUS: VIEWS
--
-- A view is a SAVED QUERY that behaves like a table.
-- Why useful: (1) hides complicated joins, (2) one place to fix a query,
--             (3) security - give users the view, not the base tables.
--
-- Project use : Results page, Attendance page and Faculty page read these views.
-- Demo        : SELECT * FROM student_result_view WHERE student_id = 101;
-- =====================================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- STUDENT -> DEPARTMENT, RESULT -> SUBJECT  (joins four tables)
CREATE OR REPLACE VIEW student_result_view AS
SELECT s.student_id,
       s.student_name,
       d.dept_code,
       sb.subject_code,
       sb.subject_name,
       r.semester,
       r.internal_mark,
       r.semester_mark,
       r.final_mark,
       r.grade_code,
       r.grade_point,
       r.credits,
       r.pass_fail
  FROM result r
  JOIN student    s  ON s.student_id = r.student_id
  JOIN department d  ON d.dept_id    = s.dept_id
  JOIN subject    sb ON sb.subject_id = r.subject_id;

-- Attendance summary per student per subject (aggregate + GROUP BY)
CREATE OR REPLACE VIEW student_attendance_view AS
SELECT s.student_id,
       s.student_name,
       sb.subject_id,
       sb.subject_code,
       COUNT(*)                                                         AS total_sessions,
       SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END)           AS present_sessions,
       ROUND(SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) AS attendance_pct
  FROM attendance_record  ar
  JOIN attendance_session se ON se.session_id = ar.session_id
  JOIN student            s  ON s.student_id  = ar.student_id
  JOIN subject            sb ON sb.subject_id = se.subject_id
 GROUP BY s.student_id, s.student_name, sb.subject_id, sb.subject_code;

-- Who teaches what to which class
CREATE OR REPLACE VIEW faculty_subject_view AS
SELECT f.faculty_id,
       f.faculty_code,
       f.faculty_name,
       c.class_name,
       sb.subject_code,
       sb.subject_name,
       sb.credits
  FROM class_subject cs
  JOIN faculty f  ON f.faculty_id  = cs.faculty_id
  JOIN class   c  ON c.class_id    = cs.class_id
  JOIN subject sb ON sb.subject_id = cs.subject_id;

-- CGPA per student (uses the stored function)
CREATE OR REPLACE VIEW student_cgpa_view AS
SELECT s.student_id,
       s.student_name,
       d.dept_code,
       calculate_cgpa(s.student_id)       AS cgpa,
       calculate_attendance(s.student_id) AS overall_attendance_pct
  FROM student s
  JOIN department d ON d.dept_id = s.dept_id;

PROMPT === 12_views.sql finished ===
