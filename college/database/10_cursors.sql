-- =====================================================================
-- 10_cursors.sql                       SYLLABUS: IMPLICIT cursor, EXPLICIT cursor
--
-- A cursor is a pointer to the rows a SQL statement returns.
--   IMPLICIT : Oracle opens/closes it for you for every INSERT/UPDATE/DELETE/SELECT INTO.
--              You read its state with SQL%FOUND, SQL%NOTFOUND, SQL%ROWCOUNT.
--   EXPLICIT : you declare it and control it:  DECLARE CURSOR -> OPEN -> FETCH -> EXIT -> CLOSE
--
-- Project use : department_report_text  (explicit cursor)  -> "Department report" in the app
--               demo_implicit_cursor    (SQL%ROWCOUNT ...) -> "Implicit cursor" topic in DBMS Lab
-- Demo        : run this file in SQL*Plus (SET SERVEROUTPUT ON) and read the output lines.
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON

-- ---------------------------------------------------------------------
-- EXPLICIT cursor with a parameter, wrapped in a procedure that returns the report as text
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE department_report_text (
  p_dept_id IN  NUMBER,
  p_report  OUT VARCHAR2
) IS
  -- 1) DECLARE the cursor (parameter = department id)
  CURSOR c_students (cp_dept NUMBER) IS
    SELECT s.student_id, s.student_name,
           calculate_cgpa(s.student_id)       AS cgpa,
           calculate_attendance(s.student_id) AS att_pct
      FROM student s
     WHERE s.dept_id = cp_dept
     ORDER BY s.student_id;

  v_row       c_students%ROWTYPE;
  v_dept_name department.dept_name%TYPE;
  v_count     NUMBER := 0;
  v_nl        CONSTANT VARCHAR2(1) := CHR(10);
BEGIN
  BEGIN
    SELECT dept_name INTO v_dept_name FROM department WHERE dept_id = p_dept_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE_APPLICATION_ERROR(-20040, 'Department ' || p_dept_id || ' not found');
  END;

  p_report := 'DEPARTMENT REPORT - ' || v_dept_name || v_nl
           || RPAD('ID', 6) || RPAD('NAME', 14) || RPAD('CGPA', 8) || 'ATTENDANCE%' || v_nl;

  OPEN c_students(p_dept_id);                       -- 2) OPEN
  LOOP
    FETCH c_students INTO v_row;                    -- 3) FETCH one row
    EXIT WHEN c_students%NOTFOUND;                  -- 4) EXIT when no more rows
    v_count := v_count + 1;
    p_report := p_report
             || RPAD(TO_CHAR(v_row.student_id), 6)
             || RPAD(v_row.student_name, 14)
             || RPAD(NVL(TO_CHAR(v_row.cgpa, 'FM990.00'), '-'), 8)
             || NVL(TO_CHAR(v_row.att_pct, 'FM990.00'), '-') || v_nl;
  END LOOP;
  CLOSE c_students;                                 -- 5) CLOSE

  p_report := p_report || 'Total students: ' || v_count;
END department_report_text;
/

-- ---------------------------------------------------------------------
-- IMPLICIT cursor attributes (nothing is changed: the update sets phone = phone and is rolled back)
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE demo_implicit_cursor (p_out OUT VARCHAR2) IS
  v_nl CONSTANT VARCHAR2(1) := CHR(10);
BEGIN
  UPDATE student SET phone = phone WHERE dept_id = 99;      -- no such department
  p_out := 'UPDATE ... WHERE dept_id = 99' || v_nl
        || '  SQL%FOUND    = ' || CASE WHEN SQL%FOUND    THEN 'TRUE' ELSE 'FALSE' END || v_nl
        || '  SQL%NOTFOUND = ' || CASE WHEN SQL%NOTFOUND THEN 'TRUE' ELSE 'FALSE' END || v_nl
        || '  SQL%ROWCOUNT = ' || SQL%ROWCOUNT || v_nl;

  UPDATE student SET phone = phone WHERE dept_id = 1;       -- CSE students
  p_out := p_out || 'UPDATE ... WHERE dept_id = 1' || v_nl
        || '  SQL%FOUND    = ' || CASE WHEN SQL%FOUND    THEN 'TRUE' ELSE 'FALSE' END || v_nl
        || '  SQL%NOTFOUND = ' || CASE WHEN SQL%NOTFOUND THEN 'TRUE' ELSE 'FALSE' END || v_nl
        || '  SQL%ROWCOUNT = ' || SQL%ROWCOUNT;
  ROLLBACK;
END demo_implicit_cursor;
/

-- =====================================================================
-- DEMO BLOCKS (run these in SQL*Plus)
-- =====================================================================
PROMPT --- Implicit cursor demo
DECLARE
  v_msg VARCHAR2(500);
BEGIN
  demo_implicit_cursor(v_msg);
  DBMS_OUTPUT.PUT_LINE(v_msg);
END;
/

PROMPT --- Explicit cursor demo (CSE department report)
DECLARE
  v_report VARCHAR2(4000);
BEGIN
  department_report_text(1, v_report);
  DBMS_OUTPUT.PUT_LINE(v_report);
END;
/

PROMPT --- Cursor FOR loop (Oracle opens, fetches and closes the cursor for you)
BEGIN
  FOR r IN (SELECT dept_code, dept_name FROM department ORDER BY dept_id) LOOP
    DBMS_OUTPUT.PUT_LINE(RPAD(r.dept_code, 6) || r.dept_name);
  END LOOP;
END;
/

PROMPT === 10_cursors.sql finished ===
