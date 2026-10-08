-- =====================================================================
-- 11_exceptions.sql       SYLLABUS: PL/SQL predefined + non-predefined exceptions
--
--   Normal execution -> exception occurs -> control jumps to EXCEPTION block -> handled
--
--   PREDEFINED     : NO_DATA_FOUND, TOO_MANY_ROWS, DUP_VAL_ON_INDEX (Oracle gives them a name)
--   NON-PREDEFINED : Oracle error without a name. We give it one:
--                      e_child_found EXCEPTION;
--                      PRAGMA EXCEPTION_INIT(e_child_found, -2292);   -- ORA-02292
--   USER-DEFINED   : RAISE_APPLICATION_ERROR(-20xxx, 'message')
--
-- Project use : the app's DBMS Lab calls demo_exception(case) and shows the message.
--               Spring's GlobalExceptionHandler turns ORA-20001 etc. into HTTP 400/409.
-- Demo        : BEGIN demo_exception('NO_DATA_FOUND', :msg); END;  (or run this file)
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON

CREATE OR REPLACE PROCEDURE demo_exception (
  p_case IN  VARCHAR2,
  p_out  OUT VARCHAR2
) IS
  e_child_found   EXCEPTION;
  PRAGMA EXCEPTION_INIT(e_child_found,   -2292);   -- child record found (non-predefined)
  e_parent_missing EXCEPTION;
  PRAGMA EXCEPTION_INIT(e_parent_missing, -2291);  -- parent key not found (non-predefined)
  v_name  student.student_name%TYPE;
  v_dummy NUMBER;
BEGIN
  IF p_case = 'NO_DATA_FOUND' THEN
    SELECT student_name INTO v_name FROM student WHERE student_id = 999999;   -- no such student
    p_out := 'not reached';

  ELSIF p_case = 'TOO_MANY_ROWS' THEN
    SELECT student_name INTO v_name FROM student WHERE dept_id = 1;           -- 6 rows into 1 variable
    p_out := 'not reached';

  ELSIF p_case = 'DUP_VAL_ON_INDEX' THEN
    INSERT INTO department (dept_id, dept_code, dept_name) VALUES (99, 'CSE', 'Duplicate code test');
    p_out := 'not reached';

  ELSIF p_case = 'CHILD_RECORD_FOUND' THEN
    DELETE FROM department WHERE dept_id = 1;                                  -- students refer to it
    p_out := 'not reached';

  ELSIF p_case = 'PARENT_NOT_FOUND' THEN
    INSERT INTO student (student_id, student_name, email, dept_id, class_id)
    VALUES (998, 'Ghost', 'ghost@college.edu', 77, 1);                         -- department 77 missing
    p_out := 'not reached';

  ELSIF p_case = 'USER_DEFINED' THEN
    UPDATE marks SET raw_marks = 999
     WHERE student_id = 101 AND subject_id = 2 AND assessment_id = 2;          -- trigger raises -20001
    p_out := 'not reached';

  ELSE
    p_out := 'Unknown case. Use NO_DATA_FOUND, TOO_MANY_ROWS, DUP_VAL_ON_INDEX, '
          || 'CHILD_RECORD_FOUND, PARENT_NOT_FOUND or USER_DEFINED';
  END IF;

EXCEPTION
  WHEN NO_DATA_FOUND THEN
    p_out := 'Handled NO_DATA_FOUND (ORA-01403): the SELECT INTO found zero rows.';
  WHEN TOO_MANY_ROWS THEN
    p_out := 'Handled TOO_MANY_ROWS (ORA-01422): the SELECT INTO found more than one row.';
  WHEN DUP_VAL_ON_INDEX THEN
    p_out := 'Handled DUP_VAL_ON_INDEX (ORA-00001): a UNIQUE / PRIMARY KEY value already exists.';
  WHEN e_child_found THEN
    ROLLBACK;
    p_out := 'Handled NON-PREDEFINED ORA-02292 (e_child_found): cannot delete, child rows exist.';
  WHEN e_parent_missing THEN
    ROLLBACK;
    p_out := 'Handled NON-PREDEFINED ORA-02291 (e_parent_missing): parent key does not exist.';
  WHEN OTHERS THEN
    ROLLBACK;
    IF SQLCODE = -20001 THEN
      p_out := 'Handled USER-DEFINED error ' || SQLCODE || ': ' || SQLERRM;
    ELSE
      p_out := 'Handled OTHERS ' || SQLCODE || ': ' || SQLERRM;
    END IF;
END demo_exception;
/

-- =====================================================================
-- DEMO BLOCKS
-- =====================================================================
DECLARE
  v_msg VARCHAR2(1000);
BEGIN
  FOR c IN (SELECT column_value AS case_name FROM TABLE(SYS.ODCIVARCHAR2LIST(
              'NO_DATA_FOUND', 'TOO_MANY_ROWS', 'DUP_VAL_ON_INDEX',
              'CHILD_RECORD_FOUND', 'PARENT_NOT_FOUND', 'USER_DEFINED'))) LOOP
    demo_exception(c.case_name, v_msg);
    DBMS_OUTPUT.PUT_LINE(RPAD(c.case_name, 20) || ' -> ' || v_msg);
  END LOOP;
  ROLLBACK;
END;
/

PROMPT === 11_exceptions.sql finished ===
