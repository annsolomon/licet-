-- =====================================================================
-- 06_functions.sql                              SYLLABUS: Oracle FUNCTIONS (PL/SQL)
--
-- A function takes INPUT, does a CALCULATION and RETURNS one value:
--     input  ->  FUNCTION  ->  calculation  ->  return value
--
-- Functions in this file:
--   calculate_attendance(student [, subject])  -> attendance %
--   calculate_internal_mark(student, subject)  -> /40   (EXACT syllabus formula)
--   calculate_semester_mark(student, subject)  -> /60
--   calculate_grade(final_mark)                -> 'A+'
--   get_grade_point(grade)                     -> 9
--   calculate_sgpa(student, semester)          -> SGPA
--   calculate_cgpa(student)                    -> CGPA
--
-- Project use : called from SQL (views, queries) and from the procedures in 07.
-- Demo        : SELECT calculate_attendance(101, 2) FROM dual;
--               SELECT calculate_internal_mark(101, 2) FROM dual;     --> 33.4
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON

-- ---------------------------------------------------------------------
-- Attendance % = PRESENT / TOTAL * 100   (OD / MEDICAL / OTHER do not count as present)
-- p_subject_id = NULL  -> over all subjects
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION calculate_attendance (
  p_student_id IN NUMBER,
  p_subject_id IN NUMBER DEFAULT NULL
) RETURN NUMBER IS
  v_total   NUMBER;
  v_present NUMBER;
BEGIN
  SELECT COUNT(*),
         NVL(SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END), 0)
    INTO v_total, v_present
    FROM attendance_record r
    JOIN attendance_session s ON s.session_id = r.session_id
   WHERE r.student_id = p_student_id
     AND (p_subject_id IS NULL OR s.subject_id = p_subject_id);

  IF v_total = 0 THEN
    RETURN NULL;                       -- no sessions yet
  END IF;
  RETURN ROUND(v_present / v_total * 100, 2);
END calculate_attendance;
/

-- ---------------------------------------------------------------------
-- INTERNAL /40
--   part1 = ASG1 + (CT1 + CAT1) * 2/3          (/100)
--   part2 = ASG2 + (CT2 + CAT2) * 2/3          (/100)
--   internal = (part1 + part2) / 5             (/40)
-- Returns NULL when any of the six internal marks is missing.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION calculate_internal_mark (
  p_student_id IN NUMBER,
  p_subject_id IN NUMBER
) RETURN NUMBER IS
  v_asg1 NUMBER; v_ct1 NUMBER; v_cat1 NUMBER;
  v_asg2 NUMBER; v_ct2 NUMBER; v_cat2 NUMBER;
  v_part1 NUMBER; v_part2 NUMBER;
BEGIN
  SELECT MAX(CASE a.assessment_code WHEN 'ASG1' THEN m.raw_marks END),
         MAX(CASE a.assessment_code WHEN 'CT1'  THEN m.raw_marks END),
         MAX(CASE a.assessment_code WHEN 'CAT1' THEN m.raw_marks END),
         MAX(CASE a.assessment_code WHEN 'ASG2' THEN m.raw_marks END),
         MAX(CASE a.assessment_code WHEN 'CT2'  THEN m.raw_marks END),
         MAX(CASE a.assessment_code WHEN 'CAT2' THEN m.raw_marks END)
    INTO v_asg1, v_ct1, v_cat1, v_asg2, v_ct2, v_cat2
    FROM marks m
    JOIN assessment a ON a.assessment_id = m.assessment_id
   WHERE m.student_id = p_student_id
     AND m.subject_id = p_subject_id;

  IF v_asg1 IS NULL OR v_ct1 IS NULL OR v_cat1 IS NULL
     OR v_asg2 IS NULL OR v_ct2 IS NULL OR v_cat2 IS NULL THEN
    RETURN NULL;
  END IF;

  v_part1 := v_asg1 + (v_ct1 + v_cat1) * 2 / 3;
  v_part2 := v_asg2 + (v_ct2 + v_cat2) * 2 / 3;
  RETURN ROUND((v_part1 + v_part2) / 5, 2);
END calculate_internal_mark;
/

-- SEMESTER /60  = SEM exam (/100) * 0.6
CREATE OR REPLACE FUNCTION calculate_semester_mark (
  p_student_id IN NUMBER,
  p_subject_id IN NUMBER
) RETURN NUMBER IS
  v_sem NUMBER;
BEGIN
  SELECT m.raw_marks
    INTO v_sem
    FROM marks m
    JOIN assessment a ON a.assessment_id = m.assessment_id
   WHERE m.student_id = p_student_id
     AND m.subject_id = p_subject_id
     AND a.assessment_code = 'SEM';
  RETURN ROUND(v_sem * 0.6, 2);
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    RETURN NULL;
END calculate_semester_mark;
/

-- Grade lookup from the GRADE table (configurable, nothing hard-coded here)
CREATE OR REPLACE FUNCTION calculate_grade (p_final_mark IN NUMBER) RETURN VARCHAR2 IS
  v_grade grade.grade_code%TYPE;
BEGIN
  SELECT grade_code INTO v_grade
    FROM grade
   WHERE ROUND(p_final_mark) BETWEEN min_mark AND max_mark;
  RETURN v_grade;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    RETURN NULL;                       -- mark outside 0..100
END calculate_grade;
/

CREATE OR REPLACE FUNCTION get_grade_point (p_grade IN VARCHAR2) RETURN NUMBER IS
  v_gp grade.grade_point%TYPE;
BEGIN
  SELECT grade_point INTO v_gp FROM grade WHERE grade_code = p_grade;
  RETURN v_gp;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    RETURN NULL;
END get_grade_point;
/

-- SGPA = SUM(grade_point * credits) / SUM(credits)  for ONE semester
CREATE OR REPLACE FUNCTION calculate_sgpa (
  p_student_id IN NUMBER,
  p_semester   IN NUMBER
) RETURN NUMBER IS
  v_points  NUMBER;
  v_credits NUMBER;
BEGIN
  SELECT SUM(grade_point * credits), SUM(credits)
    INTO v_points, v_credits
    FROM result
   WHERE student_id = p_student_id
     AND semester   = p_semester;
  IF v_credits IS NULL OR v_credits = 0 THEN
    RETURN NULL;
  END IF;
  RETURN ROUND(v_points / v_credits, 2);
END calculate_sgpa;
/

-- CGPA = same formula over ALL semesters
CREATE OR REPLACE FUNCTION calculate_cgpa (p_student_id IN NUMBER) RETURN NUMBER IS
  v_points  NUMBER;
  v_credits NUMBER;
BEGIN
  SELECT SUM(grade_point * credits), SUM(credits)
    INTO v_points, v_credits
    FROM result
   WHERE student_id = p_student_id;
  IF v_credits IS NULL OR v_credits = 0 THEN
    RETURN NULL;
  END IF;
  RETURN ROUND(v_points / v_credits, 2);
END calculate_cgpa;
/

-- ---------------- quick self-check (shown when the script runs) ----------------
PROMPT Rahul (101) CS302 internal mark - expected 33.4:
SELECT calculate_internal_mark(101, 2) AS internal_mark FROM dual;
PROMPT Rahul (101) CS302 semester mark - expected 49.2:
SELECT calculate_semester_mark(101, 2) AS semester_mark FROM dual;
PROMPT Grade for 82.6 - expected A+:
SELECT calculate_grade(82.6) AS grade FROM dual;
PROMPT === 06_functions.sql finished ===
