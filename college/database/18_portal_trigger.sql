-- =====================================================================
-- 18_portal_trigger.sql   (run as COLLEGE, after 17)
-- TRIGGER trg_attendance_notify: every time a period is marked ABSENT (insert or update, from ANY
-- application) a PARENT and an ADVISOR notification row is created with status PENDING.
-- The notification service (port 8085) picks the PENDING rows up and sends them.
-- Then the absences of the latest day are queued, so the portals open with real notifications.
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

CREATE OR REPLACE TRIGGER trg_attendance_notify
AFTER INSERT OR UPDATE OF status ON attendance_record
FOR EACH ROW
WHEN (NEW.status = 'ABSENT')
BEGIN
  notify_absence(:NEW.record_id, :NEW.session_id, :NEW.student_id);
END;
/

DECLARE
  v_count NUMBER := 0;
BEGIN
  FOR r IN (SELECT ar.record_id, ar.session_id, ar.student_id
              FROM attendance_record ar
              JOIN attendance_session se ON se.session_id = ar.session_id
             WHERE ar.status = 'ABSENT'
               AND se.session_date = (SELECT MAX(session_date) FROM attendance_session)) LOOP
    notify_absence(r.record_id, r.session_id, r.student_id);
    v_count := v_count + 1;
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('Absences of the latest day queued: ' || v_count);
END;
/

DELETE FROM audit_log;
COMMIT;

PROMPT === 18_portal_trigger.sql finished ===
