-- =====================================================================
-- 09_triggers.sql                 SYLLABUS: INSERT trigger, UPDATE trigger, DELETE trigger
--
-- A trigger is PL/SQL that Oracle runs AUTOMATICALLY when a table changes:
--     UPDATE MARKS  ->  TRIGGER fires by itself  ->  row appears in AUDIT_LOG
--
--   trg_marks_validate      BEFORE INSERT/UPDATE on MARKS  rejects marks above the maximum (ORA-20001)
--   trg_marks_audit_update  AFTER  UPDATE on MARKS         writes old/new value to AUDIT_LOG   (UPDATE trigger)
--   trg_student_audit_ins   AFTER  INSERT on STUDENT       writes AUDIT_LOG                    (INSERT trigger)
--   trg_student_audit_del   AFTER  DELETE on STUDENT       writes AUDIT_LOG                    (DELETE trigger)
--
-- Demo (run in SQL*Plus, or open the "DBMS Lab" page in the app):
--   UPDATE marks SET raw_marks = raw_marks + 1 WHERE student_id = 101 AND subject_id = 2 AND assessment_id = 2;
--   SELECT * FROM audit_log ORDER BY audit_id DESC;
--   ROLLBACK;
-- "changed_by" is the application user: Java calls DBMS_SESSION.SET_IDENTIFIER before the
-- update; if nobody did, the database user name is used.
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON

-- ---------- validation trigger (BEFORE) ----------
CREATE OR REPLACE TRIGGER trg_marks_validate
BEFORE INSERT OR UPDATE OF raw_marks ON marks
FOR EACH ROW
DECLARE
  v_max assessment.max_marks%TYPE;
BEGIN
  SELECT max_marks INTO v_max
    FROM assessment
   WHERE assessment_id = :NEW.assessment_id;

  IF :NEW.raw_marks > v_max THEN
    RAISE_APPLICATION_ERROR(-20001,
      'Marks ' || :NEW.raw_marks || ' exceed the maximum ' || v_max || ' for this assessment');
  END IF;
END;
/

-- ---------- UPDATE trigger ----------
CREATE OR REPLACE TRIGGER trg_marks_audit_update
AFTER UPDATE OF raw_marks ON marks
FOR EACH ROW
WHEN (OLD.raw_marks <> NEW.raw_marks)
BEGIN
  INSERT INTO audit_log (audit_id, table_name, operation, student_id, record_key,
                         old_value, new_value, changed_by, changed_date)
  VALUES (seq_audit.NEXTVAL, 'MARKS', 'UPDATE', :NEW.student_id,
          'subject=' || :NEW.subject_id || ',assessment=' || :NEW.assessment_id,
          TO_CHAR(:OLD.raw_marks), TO_CHAR(:NEW.raw_marks),
          NVL(SYS_CONTEXT('USERENV', 'CLIENT_IDENTIFIER'), SYS_CONTEXT('USERENV', 'SESSION_USER')),
          SYSTIMESTAMP);
END;
/

-- ---------- INSERT trigger ----------
CREATE OR REPLACE TRIGGER trg_student_audit_ins
AFTER INSERT ON student
FOR EACH ROW
BEGIN
  INSERT INTO audit_log (audit_id, table_name, operation, student_id, record_key,
                         old_value, new_value, changed_by, changed_date)
  VALUES (seq_audit.NEXTVAL, 'STUDENT', 'INSERT', :NEW.student_id,
          'student_id=' || :NEW.student_id,
          NULL, :NEW.student_name || ' <' || :NEW.email || '>',
          NVL(SYS_CONTEXT('USERENV', 'CLIENT_IDENTIFIER'), SYS_CONTEXT('USERENV', 'SESSION_USER')),
          SYSTIMESTAMP);
END;
/

-- ---------- DELETE trigger ----------
CREATE OR REPLACE TRIGGER trg_student_audit_del
AFTER DELETE ON student
FOR EACH ROW
BEGIN
  INSERT INTO audit_log (audit_id, table_name, operation, student_id, record_key,
                         old_value, new_value, changed_by, changed_date)
  VALUES (seq_audit.NEXTVAL, 'STUDENT', 'DELETE', :OLD.student_id,
          'student_id=' || :OLD.student_id,
          :OLD.student_name || ' <' || :OLD.email || '>', NULL,
          NVL(SYS_CONTEXT('USERENV', 'CLIENT_IDENTIFIER'), SYS_CONTEXT('USERENV', 'SESSION_USER')),
          SYSTIMESTAMP);
END;
/

PROMPT === 09_triggers.sql finished ===
