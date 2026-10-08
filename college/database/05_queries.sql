-- =====================================================================
-- 05_queries.sql       SYLLABUS: WHERE, aggregates, GROUP BY, HAVING, set operations,
--                      joins (INNER / LEFT / RIGHT / multi-table), subqueries
--
-- Run AFTER ./setup-db.sh (needs the calculated RESULT rows):   ./demo-sql.sh 05
-- The same queries are runnable from the app: DBMS Lab page.
-- =====================================================================
SET DEFINE OFF
SET LINESIZE 200
SET PAGESIZE 50
COLUMN student_name FORMAT A12
COLUMN subject_name FORMAT A36
COLUMN dept_name    FORMAT A40
COLUMN dept_code    FORMAT A6

PROMPT ===== WHERE: CSE students whose name starts with R =====
SELECT student_id, student_name, email FROM student WHERE dept_id = 1 AND student_name LIKE 'R%';

PROMPT ===== AGGREGATES: count, average, max, min, sum =====
SELECT COUNT(*) AS students, ROUND(AVG(final_mark), 2) AS avg_final, MAX(final_mark) AS best, MIN(final_mark) AS lowest
  FROM result WHERE subject_id = 2;
SELECT SUM(net) AS total_net_payroll FROM payroll WHERE pay_month = '2026-09';

PROMPT ===== GROUP BY: students per department =====
SELECT d.dept_code, COUNT(s.student_id) AS students
  FROM department d LEFT JOIN student s ON s.dept_id = d.dept_id
 GROUP BY d.dept_code ORDER BY d.dept_code;

PROMPT ===== HAVING: departments with more than 2 students =====
SELECT d.dept_code, COUNT(*) AS students
  FROM department d JOIN student s ON s.dept_id = d.dept_id
 GROUP BY d.dept_code HAVING COUNT(*) > 2;

PROMPT ===== SET OPERATIONS =====
PROMPT UNION     - every student who FAILED any subject OR has overall attendance below 75
SELECT student_id FROM result WHERE pass_fail = 'FAIL'
UNION
SELECT student_id FROM student WHERE calculate_attendance(student_id) < 75;
PROMPT INTERSECT - students who failed AND have attendance below 75
SELECT student_id FROM result WHERE pass_fail = 'FAIL'
INTERSECT
SELECT student_id FROM student WHERE calculate_attendance(student_id) < 75;
PROMPT MINUS     - students with NO failed subject
SELECT student_id FROM student
MINUS
SELECT student_id FROM result WHERE pass_fail = 'FAIL';

PROMPT ===== INNER JOIN: student -> department =====
SELECT s.student_id, s.student_name, d.dept_code
  FROM student s INNER JOIN department d ON d.dept_id = s.dept_id;

PROMPT ===== LEFT JOIN: every department, even when it has no faculty-subject mapping =====
SELECT d.dept_code, COUNT(sb.subject_id) AS subjects
  FROM department d LEFT JOIN subject sb ON sb.dept_id = d.dept_id
 GROUP BY d.dept_code ORDER BY d.dept_code;

PROMPT ===== RIGHT JOIN: every subject, with the students who have results in it =====
SELECT sb.subject_code, COUNT(r.result_id) AS results
  FROM result r RIGHT JOIN subject sb ON sb.subject_id = r.subject_id
 GROUP BY sb.subject_code ORDER BY sb.subject_code;

PROMPT ===== MULTI-TABLE JOIN: STUDENT -> DEPARTMENT -> CLASS -> SUBJECT -> MARKS =====
SELECT s.student_name, d.dept_code, c.class_name, sb.subject_code, a.assessment_code, m.raw_marks
  FROM marks m
  JOIN student    s  ON s.student_id   = m.student_id
  JOIN department d  ON d.dept_id      = s.dept_id
  JOIN class      c  ON c.class_id     = s.class_id
  JOIN subject    sb ON sb.subject_id  = m.subject_id
  JOIN assessment a  ON a.assessment_id = m.assessment_id
 WHERE s.student_id = 101 AND sb.subject_code = 'CS302'
 ORDER BY a.assessment_id;

PROMPT ===== SUBQUERY 1: students scoring ABOVE the average in CS302 =====
SELECT s.student_id, s.student_name, r.final_mark
  FROM result r JOIN student s ON s.student_id = r.student_id
 WHERE r.subject_id = 2
   AND r.final_mark > (SELECT AVG(final_mark) FROM result WHERE subject_id = 2);

PROMPT ===== SUBQUERY 2: CSE-III-A students BELOW the class average attendance =====
SELECT student_id, student_name, calculate_attendance(student_id) AS attendance_pct
  FROM student
 WHERE class_id = 1
   AND calculate_attendance(student_id) < (SELECT AVG(calculate_attendance(student_id)) FROM student WHERE class_id = 1);

PROMPT ===== SUBQUERY 3: highest scoring result =====
SELECT s.student_name, sb.subject_code, r.final_mark
  FROM result r JOIN student s ON s.student_id = r.student_id JOIN subject sb ON sb.subject_id = r.subject_id
 WHERE r.final_mark = (SELECT MAX(final_mark) FROM result);

PROMPT ===== SUBQUERY 4 (correlated): marks greater than the student's DEPARTMENT average =====
SELECT s.student_name, d.dept_code, sb.subject_code, r.final_mark
  FROM result r
  JOIN student s    ON s.student_id = r.student_id
  JOIN department d ON d.dept_id    = s.dept_id
  JOIN subject sb   ON sb.subject_id = r.subject_id
 WHERE r.final_mark > (SELECT AVG(r2.final_mark)
                         FROM result r2 JOIN student s2 ON s2.student_id = r2.student_id
                        WHERE s2.dept_id = s.dept_id)
 ORDER BY d.dept_code, r.final_mark DESC;

PROMPT ===== FUNCTION used inside SQL: attendance and CGPA of every CSE student =====
SELECT student_id, student_name, calculate_attendance(student_id) AS attendance_pct, calculate_cgpa(student_id) AS cgpa
  FROM student WHERE dept_id = 1 ORDER BY student_id;

PROMPT === 05_queries.sql finished ===
