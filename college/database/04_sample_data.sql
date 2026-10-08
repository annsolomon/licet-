-- =====================================================================
-- 04_sample_data.sql                                   SYLLABUS: DML (INSERT, UPDATE)
-- Fixed, repeatable demo data. Everything here is deterministic so the numbers
-- in the documentation always match what you see on screen.
--
--   4 departments, 6 faculty, 8 subjects, 4 classes, 10 students,
--   5 employees, 72 attendance sessions, ~1000 attendance records, ~290 mark rows.
--
-- IMPORTANT DEMO ROW:  Rahul (101) in CS302 gets exactly the marks of the syllabus
-- example so that the calculation shows 33.4 + 49.2 = 82.6.
-- =====================================================================
SET DEFINE OFF
SET FEEDBACK ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- ---------- login accounts (password = SHA-256('college-salt:' || password)) ----------
INSERT INTO app_user (user_id, username, password_hash, full_name, role_name)
VALUES (1, 'admin',   LOWER(RAWTOHEX(STANDARD_HASH('college-salt:admin123',   'SHA256'))), 'System Administrator', 'ADMIN');
INSERT INTO app_user (user_id, username, password_hash, full_name, role_name)
VALUES (2, 'faculty', LOWER(RAWTOHEX(STANDARD_HASH('college-salt:faculty123', 'SHA256'))), 'Faculty Demo User',    'FACULTY');

-- ---------- departments ----------
INSERT INTO department (dept_id, dept_code, dept_name) VALUES (1, 'CSE',  'Computer Science and Engineering');
INSERT INTO department (dept_id, dept_code, dept_name) VALUES (2, 'ECE',  'Electronics and Communication Engineering');
INSERT INTO department (dept_id, dept_code, dept_name) VALUES (3, 'EEE',  'Electrical and Electronics Engineering');
INSERT INTO department (dept_id, dept_code, dept_name) VALUES (4, 'MECH', 'Mechanical Engineering');

-- ---------- faculty ----------
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (1, 'F001', 'Kumar',   'kumar@college.edu',   'Professor',           1);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (2, 'F002', 'Anitha',  'anitha@college.edu',  'Associate Professor', 1);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (3, 'F003', 'Suresh',  'suresh@college.edu',  'Assistant Professor', 1);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (4, 'F004', 'Meena',   'meena@college.edu',   'Professor',           2);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (5, 'F005', 'Rajesh',  'rajesh@college.edu',  'Associate Professor', 3);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (6, 'F006', 'Lakshmi', 'lakshmi@college.edu', 'Assistant Professor', 4);

-- ---------- subjects ----------
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (1, 'CS301', 'Data Structures',                     4, 3, 1);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (2, 'CS302', 'Database Management Systems',        4, 3, 1);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (3, 'CS303', 'Object Oriented Programming (Java)', 3, 3, 1);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (4, 'EC301', 'Digital Electronics',               4, 3, 2);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (5, 'EE301', 'Circuit Theory',                    3, 3, 3);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (6, 'ME301', 'Thermodynamics',                    3, 3, 4);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (7, 'CS201', 'Discrete Mathematics',              4, 2, 1);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (8, 'CS202', 'Programming in C',                  3, 2, 1);

-- ---------- classes (one section per department, semester 3) ----------
INSERT INTO class (class_id, class_name, dept_id, semester, section_name, academic_year) VALUES (1, 'CSE-III-A',  1, 3, 'A', '2026-2027');
INSERT INTO class (class_id, class_name, dept_id, semester, section_name, academic_year) VALUES (2, 'ECE-III-A',  2, 3, 'A', '2026-2027');
INSERT INTO class (class_id, class_name, dept_id, semester, section_name, academic_year) VALUES (3, 'EEE-III-A',  3, 3, 'A', '2026-2027');
INSERT INTO class (class_id, class_name, dept_id, semester, section_name, academic_year) VALUES (4, 'MECH-III-A', 4, 3, 'A', '2026-2027');

-- ---------- who teaches what to which class ----------
INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id) VALUES (1, 1, 1, 1);  -- CSE-III-A  CS301  Kumar
INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id) VALUES (2, 1, 2, 2);  -- CSE-III-A  CS302  Anitha
INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id) VALUES (3, 1, 3, 3);  -- CSE-III-A  CS303  Suresh
INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id) VALUES (4, 2, 4, 4);  -- ECE-III-A  EC301  Meena
INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id) VALUES (5, 3, 5, 5);  -- EEE-III-A  EE301  Rajesh
INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id) VALUES (6, 4, 6, 6);  -- MECH-III-A ME301  Lakshmi

-- ---------- students ----------
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (101, 'Rahul',   'rahul@college.edu',   '9000000101', 1, 1, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (102, 'Priya',   'priya@college.edu',   '9000000102', 1, 1, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (103, 'Arun',    'arun@college.edu',    '9000000103', 1, 1, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (104, 'Divya',   'divya@college.edu',   '9000000104', 1, 1, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (105, 'Karthik', 'karthik@college.edu', '9000000105', 2, 2, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (106, 'Sneha',   'sneha@college.edu',   '9000000106', 2, 2, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (107, 'Vikram',  'vikram@college.edu',  '9000000107', 3, 3, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (108, 'Deepa',   'deepa@college.edu',   '9000000108', 4, 4, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (109, 'Ravi',    'ravi@college.edu',    '9000000109', 1, 1, 2025);
INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year) VALUES (110, 'Nisha',   'nisha@college.edu',   '9000000110', 1, 1, 2025);

-- ---------- salary rules + employees ----------
INSERT INTO salary_rule (designation, hra_pct, da_pct, pf_pct) VALUES ('PROGRAMMER',          10,  5, 12);
INSERT INTO salary_rule (designation, hra_pct, da_pct, pf_pct) VALUES ('ASSISTANT_PROFESSOR', 15,  8, 12);
INSERT INTO salary_rule (designation, hra_pct, da_pct, pf_pct) VALUES ('ASSOCIATE_PROFESSOR', 18,  9, 12);
INSERT INTO salary_rule (designation, hra_pct, da_pct, pf_pct) VALUES ('PROFESSOR',           20, 10, 12);

INSERT INTO employee (emp_id, emp_name, email, designation, basic_salary) VALUES (1001, 'Dr. Ramesh Iyer',   'ramesh@college.edu',  'PROFESSOR',           80000);
INSERT INTO employee (emp_id, emp_name, email, designation, basic_salary) VALUES (1002, 'Dr. Kavitha Nair',  'kavitha@college.edu', 'ASSOCIATE_PROFESSOR', 65000);
INSERT INTO employee (emp_id, emp_name, email, designation, basic_salary) VALUES (1003, 'Mr. Arjun Das',     'arjun@college.edu',   'ASSISTANT_PROFESSOR', 50000);
INSERT INTO employee (emp_id, emp_name, email, designation, basic_salary) VALUES (1004, 'Ms. Pooja Shah',    'pooja@college.edu',   'PROGRAMMER',          45000);
INSERT INTO employee (emp_id, emp_name, email, designation, basic_salary) VALUES (1005, 'Mr. Vijay Kumar',   'vijay@college.edu',   'PROGRAMMER',          42000);

-- ---------- assessment definitions ----------
INSERT INTO assessment (assessment_id, assessment_code, assessment_name, max_marks, part_no) VALUES (1, 'ASG1', 'Assignment 1',                     40,  1);
INSERT INTO assessment (assessment_id, assessment_code, assessment_name, max_marks, part_no) VALUES (2, 'CT1',  'Class Test 1',                     30,  1);
INSERT INTO assessment (assessment_id, assessment_code, assessment_name, max_marks, part_no) VALUES (3, 'CAT1', 'Continuous Assessment Test 1',     60,  1);
INSERT INTO assessment (assessment_id, assessment_code, assessment_name, max_marks, part_no) VALUES (4, 'ASG2', 'Assignment 2',                     40,  2);
INSERT INTO assessment (assessment_id, assessment_code, assessment_name, max_marks, part_no) VALUES (5, 'CT2',  'Class Test 2',                     30,  2);
INSERT INTO assessment (assessment_id, assessment_code, assessment_name, max_marks, part_no) VALUES (6, 'CAT2', 'Continuous Assessment Test 2',     60,  2);
INSERT INTO assessment (assessment_id, assessment_code, assessment_name, max_marks, part_no) VALUES (7, 'SEM',  'Semester Examination',            100,  3);

-- ---------- grade scale (configurable; the grade is looked up with ROUND(final_mark)) ----------
INSERT INTO grade (grade_code, min_mark, max_mark, grade_point, pass_flag) VALUES ('O',  91, 100, 10, 'Y');
INSERT INTO grade (grade_code, min_mark, max_mark, grade_point, pass_flag) VALUES ('A+', 81,  90,  9, 'Y');
INSERT INTO grade (grade_code, min_mark, max_mark, grade_point, pass_flag) VALUES ('A',  71,  80,  8, 'Y');
INSERT INTO grade (grade_code, min_mark, max_mark, grade_point, pass_flag) VALUES ('B+', 61,  70,  7, 'Y');
INSERT INTO grade (grade_code, min_mark, max_mark, grade_point, pass_flag) VALUES ('B',  50,  60,  6, 'Y');
INSERT INTO grade (grade_code, min_mark, max_mark, grade_point, pass_flag) VALUES ('RA',  0,  49,  0, 'N');

-- ---------- raw marks: every student gets all 7 assessments for every subject of own department ----------
-- Formula (deterministic): raw = ROUND(max_marks * (base + MOD(student*7 + subject*3 + assessment*5, 40) / 100))
--   base = 0.55 for normal students, 0.28 for Ravi (109) so that the demo contains FAIL results.
INSERT INTO marks (marks_id, student_id, subject_id, assessment_id, raw_marks)
SELECT seq_marks.NEXTVAL, x.student_id, x.subject_id, x.assessment_id, x.raw_marks
FROM (
  SELECT s.student_id, sb.subject_id, a.assessment_id,
         ROUND(a.max_marks * (CASE WHEN s.student_id = 109 THEN 0.28 ELSE 0.55 END
                              + MOD(s.student_id * 7 + sb.subject_id * 3 + a.assessment_id * 5, 40) / 100)) AS raw_marks
  FROM student s
  JOIN subject sb ON sb.dept_id = s.dept_id
  CROSS JOIN assessment a
) x;

-- The syllabus example: Rahul / CS302  ASG1=35 CT1=27 CAT1=48 ASG2=32 CT2=24 CAT2=51 SEM=82
UPDATE marks
   SET raw_marks = DECODE(assessment_id, 1, 35, 2, 27, 3, 48, 4, 32, 5, 24, 6, 51, 7, 82)
 WHERE student_id = 101 AND subject_id = 2;

-- ---------- attendance sessions: 12 sessions for every class-subject pair ----------
INSERT INTO attendance_session (session_id, class_id, subject_id, faculty_id, session_date, period_no)
SELECT seq_att_session.NEXTVAL, cs.class_id, cs.subject_id, cs.faculty_id,
       DATE '2026-08-03' + (lv.n - 1), MOD(lv.n, 6) + 1
FROM class_subject cs
CROSS JOIN (SELECT LEVEL AS n FROM dual CONNECT BY LEVEL <= 12) lv;

-- ---------- attendance records ----------
-- day_no = 0..11 (days since 2026-08-03). Divya (104) is deliberately absent most days
-- so the LOW ATTENDANCE report has something to show; Ravi (109) misses every 4th day.
INSERT INTO attendance_record (record_id, session_id, student_id, status)
SELECT seq_att_record.NEXTVAL, y.session_id, y.student_id, y.status
FROM (
  SELECT se.session_id, st.student_id,
         CASE
           WHEN st.student_id = 104 AND MOD(se.day_no, 3) <> 0            THEN 'ABSENT'
           WHEN st.student_id = 109 AND MOD(se.day_no, 4) = 3             THEN 'ABSENT'
           WHEN MOD(st.student_id * (se.day_no + 1), 11) = 0              THEN 'OD'
           WHEN MOD(st.student_id + se.day_no, 13) = 0                    THEN 'MEDICAL'
           WHEN MOD(st.student_id * 3 + se.day_no, 9) = 0                 THEN 'ABSENT'
           ELSE 'PRESENT'
         END AS status
  FROM (SELECT session_id, class_id, session_date - DATE '2026-08-03' AS day_no FROM attendance_session) se
  JOIN student st ON st.class_id = se.class_id
) y;

COMMIT;
PROMPT === 04_sample_data.sql finished ===
