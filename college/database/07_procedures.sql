-- =====================================================================
-- 07_procedures.sql                          SYLLABUS: STORED PROCEDURES (PL/SQL)
--
-- A procedure is a named block of PL/SQL stored INSIDE Oracle and run with CALL.
--     React -> Spring Boot -> JdbcTemplate -> CALL procedure -> Oracle -> result
--
-- Procedures:
--   calculate_student_result(student, subject)   one result row (MERGE = insert-or-update)
--   calculate_all_results                        loops over every complete mark set
--   mark_attendance(session, student, status)    insert-or-update one attendance record
--   generate_payroll(emp, month, OUT payroll_id) one payslip row
--   generate_payroll_all(month, OUT created)     payroll for every employee (skips duplicates)
--
-- Demo : BEGIN calculate_student_result(101, 2); END;
--        SELECT * FROM result WHERE student_id = 101 AND subject_id = 2;
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON

-- ---------------------------------------------------------------------
-- RESULT calculation. Uses the functions from 06 so the formula lives in ONE place.
-- Error codes (RAISE_APPLICATION_ERROR): -20010 incomplete marks, -20011 invalid subject
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE calculate_student_result (
  p_student_id IN NUMBER,
  p_subject_id IN NUMBER
) IS
  v_internal NUMBER;
  v_semester NUMBER;
  v_final    NUMBER;
  v_grade    grade.grade_code%TYPE;
  v_gp       grade.grade_point%TYPE;
  v_pass     grade.pass_flag%TYPE;
  v_credits  subject.credits%TYPE;
  v_sem_no   subject.semester%TYPE;
BEGIN
  BEGIN
    SELECT credits, semester INTO v_credits, v_sem_no
      FROM subject WHERE subject_id = p_subject_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE_APPLICATION_ERROR(-20011, 'Invalid subject id ' || p_subject_id);
  END;

  v_internal := calculate_internal_mark(p_student_id, p_subject_id);
  v_semester := calculate_semester_mark(p_student_id, p_subject_id);

  IF v_internal IS NULL OR v_semester IS NULL THEN
    RAISE_APPLICATION_ERROR(-20010,
      'Marks incomplete for student ' || p_student_id || ' subject ' || p_subject_id
      || ' (need ASG1, CT1, CAT1, ASG2, CT2, CAT2 and SEM)');
  END IF;

  v_final := ROUND(v_internal + v_semester, 2);

  SELECT grade_code, grade_point, pass_flag
    INTO v_grade, v_gp, v_pass
    FROM grade
   WHERE ROUND(v_final) BETWEEN min_mark AND max_mark;

  MERGE INTO result r
  USING (SELECT p_student_id AS sid, p_subject_id AS subid FROM dual) src
     ON (r.student_id = src.sid AND r.subject_id = src.subid)
  WHEN MATCHED THEN
    UPDATE SET r.semester = v_sem_no, r.internal_mark = v_internal, r.semester_mark = v_semester,
               r.final_mark = v_final, r.grade_code = v_grade, r.grade_point = v_gp,
               r.credits = v_credits,
               r.pass_fail = CASE WHEN v_pass = 'Y' THEN 'PASS' ELSE 'FAIL' END,
               r.calculated_on = SYSDATE
  WHEN NOT MATCHED THEN
    INSERT (result_id, student_id, subject_id, semester, internal_mark, semester_mark,
            final_mark, grade_code, grade_point, credits, pass_fail, calculated_on)
    VALUES (seq_result.NEXTVAL, p_student_id, p_subject_id, v_sem_no, v_internal, v_semester,
            v_final, v_grade, v_gp, v_credits,
            CASE WHEN v_pass = 'Y' THEN 'PASS' ELSE 'FAIL' END, SYSDATE);
END calculate_student_result;
/

-- ---------------------------------------------------------------------
-- Recalculate every (student, subject) that has all 7 marks.
-- GROUP BY + HAVING picks only complete sets. An FOR-loop cursor walks the rows.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE calculate_all_results (p_count OUT NUMBER) IS
BEGIN
  p_count := 0;
  FOR rec IN (SELECT student_id, subject_id
                FROM marks
               GROUP BY student_id, subject_id
              HAVING COUNT(*) = 7
               ORDER BY student_id, subject_id) LOOP
    calculate_student_result(rec.student_id, rec.subject_id);
    p_count := p_count + 1;
  END LOOP;
END calculate_all_results;
/

-- ---------------------------------------------------------------------
-- ATTENDANCE: insert the record, or update the status if it already exists.
-- -20030 = invalid status (the CHECK constraint would also catch it, but this gives a friendly message)
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE mark_attendance (
  p_session_id IN NUMBER,
  p_student_id IN NUMBER,
  p_status     IN VARCHAR2
) IS
BEGIN
  IF p_status IS NULL OR p_status NOT IN ('PRESENT','ABSENT','OD','MEDICAL','OTHER') THEN
    RAISE_APPLICATION_ERROR(-20030, 'Invalid attendance status: ' || NVL(p_status, 'NULL'));
  END IF;

  MERGE INTO attendance_record ar
  USING (SELECT p_session_id AS sid, p_student_id AS stid FROM dual) src
     ON (ar.session_id = src.sid AND ar.student_id = src.stid)
  WHEN MATCHED THEN
    UPDATE SET ar.status = p_status
  WHEN NOT MATCHED THEN
    INSERT (record_id, session_id, student_id, status)
    VALUES (seq_att_record.NEXTVAL, p_session_id, p_student_id, p_status);
END mark_attendance;
/

-- ---------------------------------------------------------------------
-- PAYROLL for one employee and one month ('YYYY-MM').
--   gross = basic + HRA + DA        net = gross - PF
-- Percentages come from SALARY_RULE (so they are configurable).
-- -20020 = payroll already generated for that month (DUP_VAL_ON_INDEX handled)
-- -20021 = employee not found
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE generate_payroll (
  p_emp_id     IN  NUMBER,
  p_month      IN  VARCHAR2,
  p_payroll_id OUT NUMBER
) IS
  v_desig employee.designation%TYPE;
  v_hra   NUMBER(10,2);
  v_da    NUMBER(10,2);
  v_pf    NUMBER(10,2);
  v_gross NUMBER(10,2);
  v_net   NUMBER(10,2);
  v_hra_pct salary_rule.hra_pct%TYPE;
  v_da_pct  salary_rule.da_pct%TYPE;
  v_pf_pct  salary_rule.pf_pct%TYPE;
  v_basic_salary employee.basic_salary%TYPE;
BEGIN
  BEGIN
    SELECT designation, basic_salary INTO v_desig, v_basic_salary
      FROM employee WHERE emp_id = p_emp_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE_APPLICATION_ERROR(-20021, 'Employee ' || p_emp_id || ' not found');
  END;

  SELECT hra_pct, da_pct, pf_pct INTO v_hra_pct, v_da_pct, v_pf_pct
    FROM salary_rule WHERE designation = v_desig;

  v_hra   := ROUND(v_basic_salary * v_hra_pct / 100, 2);
  v_da    := ROUND(v_basic_salary * v_da_pct  / 100, 2);
  v_pf    := ROUND(v_basic_salary * v_pf_pct  / 100, 2);
  v_gross := v_basic_salary + v_hra + v_da;
  v_net   := v_gross - v_pf;

  p_payroll_id := seq_payroll.NEXTVAL;
  INSERT INTO payroll (payroll_id, emp_id, pay_month, basic, hra, da, pf, gross, net)
  VALUES (p_payroll_id, p_emp_id, p_month, v_basic_salary, v_hra, v_da, v_pf, v_gross, v_net);
EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    RAISE_APPLICATION_ERROR(-20020,
      'Payroll already generated for employee ' || p_emp_id || ' for ' || p_month);
END generate_payroll;
/

-- Payroll for every employee; employees that already have the month are skipped
CREATE OR REPLACE PROCEDURE generate_payroll_all (
  p_month   IN  VARCHAR2,
  p_created OUT NUMBER
) IS
  v_id NUMBER;
BEGIN
  p_created := 0;
  FOR e IN (SELECT emp_id FROM employee ORDER BY emp_id) LOOP
    BEGIN
      generate_payroll(e.emp_id, p_month, v_id);
      p_created := p_created + 1;
    EXCEPTION
      WHEN OTHERS THEN
        IF SQLCODE = -20020 THEN NULL; ELSE RAISE; END IF;   -- already generated: skip
    END;
  END LOOP;
END generate_payroll_all;
/

PROMPT === 07_procedures.sql finished ===
