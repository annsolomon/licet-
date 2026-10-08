-- =====================================================================
-- 17_portal_data.sql   (run as COLLEGE, after 16)
-- * 6 more faculty, 18 more subjects, so every class has 6 subjects and 3 teachers
-- * HOD of every department, class advisor of every class
-- * 60 students per department (240 in total) + one parent per student
-- * timetable: 5 days x 8 periods for every class, with period timings
-- * attendance for 8 periods a day for 10 working days (25 Sep - 8 Oct 2026)
-- * logins: hod.<dept> / hod123, advisor.<dept> / advisor123, p<student id> / parent123
-- The absence notification trigger is created later (18), so this bulk load sends nothing.
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- ---------- more faculty ----------
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (seq_faculty.NEXTVAL, 'F007', 'Harini',    'harini@college.edu',    'Assistant Professor', 2);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (seq_faculty.NEXTVAL, 'F008', 'Mohan',     'mohan@college.edu',     'Associate Professor', 2);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (seq_faculty.NEXTVAL, 'F009', 'Geetha',    'geetha@college.edu',    'Associate Professor', 3);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (seq_faculty.NEXTVAL, 'F010', 'Prakash',   'prakash@college.edu',   'Assistant Professor', 3);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (seq_faculty.NEXTVAL, 'F011', 'Sangeetha', 'sangeetha@college.edu', 'Associate Professor', 4);
INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id) VALUES (seq_faculty.NEXTVAL, 'F012', 'Dinesh',    'dinesh@college.edu',    'Assistant Professor', 4);

-- ---------- more subjects (semester 3) ----------
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'CS304', 'Computer Organization',             3, 3, 1);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'CS305', 'Operating Systems',                 3, 3, 1);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'CS306', 'Software Engineering',             3, 3, 1);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EC302', 'Signals and Systems',              3, 3, 2);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EC303', 'Electronic Circuits',              3, 3, 2);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EC304', 'Network Analysis',                3, 3, 2);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EC305', 'Electromagnetic Fields',          3, 3, 2);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EC306', 'Probability and Random Processes', 3, 3, 2);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EE302', 'Electromagnetic Theory',           3, 3, 3);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EE303', 'Electrical Machines I',            3, 3, 3);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EE304', 'Analog Electronics',              3, 3, 3);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EE305', 'Measurements and Instrumentation', 3, 3, 3);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'EE306', 'Power Generation',               3, 3, 3);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'ME302', 'Fluid Mechanics',                3, 3, 4);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'ME303', 'Strength of Materials',          3, 3, 4);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'ME304', 'Manufacturing Technology I',     3, 3, 4);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'ME305', 'Engineering Metallurgy',         3, 3, 4);
INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id) VALUES (seq_subject.NEXTVAL, 'ME306', 'Kinematics of Machinery',        3, 3, 4);

-- ---------- who teaches what (6 subjects per class) ----------
DECLARE
  PROCEDURE cs (p_class NUMBER, p_subject VARCHAR2, p_faculty VARCHAR2) IS
  BEGIN
    INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id)
    VALUES (seq_class_subject.NEXTVAL, p_class,
            (SELECT subject_id FROM subject WHERE subject_code = p_subject),
            (SELECT faculty_id FROM faculty WHERE faculty_code = p_faculty));
  END;
BEGIN
  cs(1, 'CS304', 'F001'); cs(1, 'CS305', 'F002'); cs(1, 'CS306', 'F003');
  cs(2, 'EC302', 'F007'); cs(2, 'EC303', 'F008'); cs(2, 'EC304', 'F004'); cs(2, 'EC305', 'F007'); cs(2, 'EC306', 'F008');
  cs(3, 'EE302', 'F009'); cs(3, 'EE303', 'F010'); cs(3, 'EE304', 'F005'); cs(3, 'EE305', 'F009'); cs(3, 'EE306', 'F010');
  cs(4, 'ME302', 'F011'); cs(4, 'ME303', 'F012'); cs(4, 'ME304', 'F006'); cs(4, 'ME305', 'F011'); cs(4, 'ME306', 'F012');
END;
/

-- ---------- HOD and class advisors ----------
UPDATE department SET hod_faculty_id = (SELECT faculty_id FROM faculty WHERE faculty_code = 'F001') WHERE dept_id = 1;
UPDATE department SET hod_faculty_id = (SELECT faculty_id FROM faculty WHERE faculty_code = 'F004') WHERE dept_id = 2;
UPDATE department SET hod_faculty_id = (SELECT faculty_id FROM faculty WHERE faculty_code = 'F005') WHERE dept_id = 3;
UPDATE department SET hod_faculty_id = (SELECT faculty_id FROM faculty WHERE faculty_code = 'F006') WHERE dept_id = 4;
UPDATE class SET advisor_faculty_id = (SELECT faculty_id FROM faculty WHERE faculty_code = 'F002') WHERE class_id = 1;
UPDATE class SET advisor_faculty_id = (SELECT faculty_id FROM faculty WHERE faculty_code = 'F007') WHERE class_id = 2;
UPDATE class SET advisor_faculty_id = (SELECT faculty_id FROM faculty WHERE faculty_code = 'F009') WHERE class_id = 3;
UPDATE class SET advisor_faculty_id = (SELECT faculty_id FROM faculty WHERE faculty_code = 'F011') WHERE class_id = 4;

-- ---------- students: top every department up to 60 ----------
DECLARE
  TYPE t_names IS TABLE OF VARCHAR2(30);
  v_first t_names := t_names('Aarav','Vihaan','Arjun','Sai','Reyansh','Krishna','Ishaan','Rohan','Aditya','Karan',
                             'Ananya','Diya','Meera','Kavya','Nithya','Pooja','Shruti','Janani','Lavanya','Harsha',
                             'Surya','Naveen','Bharath','Gokul','Hari','Manoj','Praveen','Yuvan','Tarun','Vignesh',
                             'Abinaya','Bhavana','Charulatha','Divya','Esha','Gayathri','Indhu','Jeeva','Keerthi','Madhu');
  v_last  t_names := t_names('Kumar','Sharma','Iyer','Nair','Reddy','Pillai','Menon','Rao','Das','Gupta',
                             'Singh','Krishnan','Subramanian','Raman','Natarajan','Venkat','Chandran','Murthy','Bose','Patel',
                             'Naidu','Shetty','Joshi','Varma','Mohan');
  v_n     NUMBER;
  v_id    NUMBER;
  v_class NUMBER;
  v_f     VARCHAR2(30);
  v_l     VARCHAR2(30);
BEGIN
  FOR d IN (SELECT dept_id FROM department ORDER BY dept_id) LOOP
    SELECT COUNT(*) INTO v_n FROM student WHERE dept_id = d.dept_id;
    SELECT class_id INTO v_class FROM class WHERE dept_id = d.dept_id;
    WHILE v_n < 60 LOOP
      v_id := seq_student.NEXTVAL;
      v_f  := v_first(MOD(v_id * 7, 40) + 1);
      v_l  := v_last(MOD(v_id * 3 + d.dept_id, 25) + 1);
      INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year)
      VALUES (v_id, v_f || ' ' || v_l, LOWER(v_f || '.' || v_l) || v_id || '@college.edu',
              '9000000' || v_id, d.dept_id, v_class, 2025);
      v_n := v_n + 1;
    END LOOP;
  END LOOP;
END;
/

-- ---------- one parent per student ----------
DECLARE
  v_pid NUMBER;
  v_sur VARCHAR2(60);
BEGIN
  FOR s IN (SELECT student_id, student_name FROM student WHERE parent_id IS NULL ORDER BY student_id) LOOP
    v_pid := seq_parent.NEXTVAL;
    v_sur := REGEXP_SUBSTR(s.student_name, '[^ ]+$');
    INSERT INTO parent (parent_id, parent_name, relation, phone, email)
    VALUES (v_pid,
            CASE MOD(s.student_id, 2) WHEN 0 THEN 'Mr. ' ELSE 'Mrs. ' END || v_sur,
            CASE MOD(s.student_id, 2) WHEN 0 THEN 'FATHER' ELSE 'MOTHER' END,
            '8' || LPAD(s.student_id, 9, '0'),
            'parent' || s.student_id || '@mail.com');
    UPDATE student SET parent_id = v_pid WHERE student_id = s.student_id;
  END LOOP;
END;
/

-- ---------- logins ----------
INSERT INTO app_user (user_id, username, password_hash, full_name, role_name, faculty_id)
SELECT seq_app_user.NEXTVAL, 'hod.' || LOWER(d.dept_code),
       LOWER(RAWTOHEX(STANDARD_HASH('college-salt:hod123', 'SHA256'))),
       f.faculty_name || ' (HOD ' || d.dept_code || ')', 'HOD', f.faculty_id
  FROM department d JOIN faculty f ON f.faculty_id = d.hod_faculty_id;

INSERT INTO app_user (user_id, username, password_hash, full_name, role_name, faculty_id)
SELECT seq_app_user.NEXTVAL, 'advisor.' || LOWER(d.dept_code),
       LOWER(RAWTOHEX(STANDARD_HASH('college-salt:advisor123', 'SHA256'))),
       f.faculty_name || ' (Advisor ' || c.class_name || ')', 'ADVISOR', f.faculty_id
  FROM class c JOIN department d ON d.dept_id = c.dept_id JOIN faculty f ON f.faculty_id = c.advisor_faculty_id;

INSERT INTO app_user (user_id, username, password_hash, full_name, role_name, parent_id)
SELECT seq_app_user.NEXTVAL, 'p' || s.student_id,
       LOWER(RAWTOHEX(STANDARD_HASH('college-salt:parent123', 'SHA256'))),
       p.parent_name || ' (parent of ' || s.student_name || ')', 'PARENT', p.parent_id
  FROM student s JOIN parent p ON p.parent_id = s.parent_id;

-- ---------- period timings and timetable ----------
INSERT INTO period_slot (period_no, start_time, end_time) VALUES (1, '09:00', '09:50');
INSERT INTO period_slot (period_no, start_time, end_time) VALUES (2, '09:50', '10:40');
INSERT INTO period_slot (period_no, start_time, end_time) VALUES (3, '10:50', '11:40');
INSERT INTO period_slot (period_no, start_time, end_time) VALUES (4, '11:40', '12:30');
INSERT INTO period_slot (period_no, start_time, end_time) VALUES (5, '13:20', '14:10');
INSERT INTO period_slot (period_no, start_time, end_time) VALUES (6, '14:10', '15:00');
INSERT INTO period_slot (period_no, start_time, end_time) VALUES (7, '15:10', '16:00');
INSERT INTO period_slot (period_no, start_time, end_time) VALUES (8, '16:00', '16:50');

INSERT INTO timetable (tt_id, class_id, day_of_week, period_no, subject_id, faculty_id)
SELECT seq_timetable.NEXTVAL, x.class_id, x.d, x.p, x.subject_id, x.faculty_id
FROM (
  SELECT cs.class_id, dl.d, pl.p, cs.subject_id, cs.faculty_id
    FROM (SELECT class_id, subject_id, faculty_id,
                 ROW_NUMBER() OVER (PARTITION BY class_id ORDER BY cs_id) AS rn
            FROM class_subject) cs
   CROSS JOIN (SELECT LEVEL AS d FROM dual CONNECT BY LEVEL <= 5) dl
   CROSS JOIN (SELECT LEVEL AS p FROM dual CONNECT BY LEVEL <= 8) pl
   WHERE cs.rn = MOD(pl.p - 1 + (dl.d - 1) * 2, 6) + 1
) x;

-- ---------- attendance: 8 periods a day, 10 working days (Fri 25 Sep .. Thu 8 Oct 2026) ----------
INSERT INTO attendance_session (session_id, class_id, subject_id, faculty_id, session_date, period_no)
SELECT seq_att_session.NEXTVAL, t.class_id, t.subject_id, t.faculty_id, dt.dd, t.period_no
  FROM (SELECT DATE '2026-09-25' + LEVEL - 1 AS dd FROM dual CONNECT BY LEVEL <= 14) dt
  JOIN timetable t ON t.day_of_week = (dt.dd - TRUNC(dt.dd, 'IW') + 1)
 WHERE dt.dd - TRUNC(dt.dd, 'IW') < 5;

INSERT INTO attendance_record (record_id, session_id, student_id, status)
SELECT seq_att_record.NEXTVAL, y.session_id, y.student_id, y.status
FROM (
  SELECT se.session_id, st.student_id,
         CASE
           WHEN st.student_id = 104 AND MOD(se.period_no + se.dayn, 3) <> 0                        THEN 'ABSENT'
           WHEN MOD(st.student_id, 19) = 0 AND MOD(se.dayn, 3) = 0                                  THEN 'ABSENT'
           WHEN MOD(st.student_id * 7 + se.period_no * 3 + se.dayn * 5 + se.subject_id, 23) = 0     THEN 'ABSENT'
           WHEN MOD(st.student_id * 5 + se.period_no + se.dayn * 3, 41) = 0                         THEN 'OD'
           WHEN MOD(st.student_id + se.dayn * 7, 53) = 0 AND se.period_no <= 4                      THEN 'MEDICAL'
           ELSE 'PRESENT'
         END AS status
    FROM (SELECT session_id, class_id, subject_id, period_no, session_date - DATE '2026-09-25' AS dayn
            FROM attendance_session WHERE session_date >= DATE '2026-09-25') se
    JOIN student st ON st.class_id = se.class_id
) y;

COMMIT;
PROMPT === 17_portal_data.sql finished ===
