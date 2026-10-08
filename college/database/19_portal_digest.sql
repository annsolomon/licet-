-- =====================================================================
-- 19_portal_digest.sql   (run as COLLEGE, after 18)
-- The class advisor gets ONE alert per marked period that lists every absentee of that period
-- (instead of one alert per absent student). Parents still get one SMS per absent student.
--   * NOTIFY_ABSENCE   : now creates the PARENT alert only
--   * NOTIFY_PERIOD    : builds / refreshes the single ADVISOR digest of one period
--   * TRG_ATTENDANCE_NOTIFY : compound trigger; collects the periods touched by a statement and
--                             builds their digests after the statement (so the whole list is complete)
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DELETE FROM notification WHERE recipient_type = 'ADVISOR';

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE notification MODIFY (student_id NULL)';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -1451 THEN RAISE; END IF;   -- already nullable
END;
/

-- the old key (record_id, recipient_type) treats two rows with record_id NULL as duplicates; digests have no record_id
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE notification DROP CONSTRAINT uq_notif_record';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -2443 THEN RAISE; END IF;   -- already dropped
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uq_notif_record_ix ON notification (CASE WHEN record_id IS NOT NULL THEN record_id END, CASE WHEN record_id IS NOT NULL THEN recipient_type END)';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE NOT IN (-955, -1408) THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uq_notif_period ON notification (CASE WHEN category = ''PERIOD'' THEN session_id END)';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE NOT IN (-955, -1408) THEN RAISE; END IF;   -- already there
END;
/

CREATE OR REPLACE PROCEDURE notify_absence (
  p_record_id  IN NUMBER,
  p_session_id IN NUMBER,
  p_student_id IN NUMBER
) IS
  v_name      student.student_name%TYPE;
  v_parent_id student.parent_id%TYPE;
  v_date      attendance_session.session_date%TYPE;
  v_period    attendance_session.period_no%TYPE;
  v_code      subject.subject_code%TYPE;
  v_sname     subject.subject_name%TYPE;
  v_phone     parent.phone%TYPE;
  v_text      VARCHAR2(300);
BEGIN
  SELECT s.student_name, s.parent_id, se.session_date, se.period_no, sb.subject_code, sb.subject_name
    INTO v_name, v_parent_id, v_date, v_period, v_code, v_sname
    FROM student s
    JOIN attendance_session se ON se.session_id = p_session_id
    JOIN subject sb            ON sb.subject_id = se.subject_id
   WHERE s.student_id = p_student_id;

  v_text := v_name || ' was ABSENT in period ' || v_period || ' - ' || v_code || ' ' || v_sname
            || ' on ' || TO_CHAR(v_date, 'DD-Mon-YYYY') || '.';

  IF v_parent_id IS NOT NULL THEN
    SELECT phone INTO v_phone FROM parent WHERE parent_id = v_parent_id;
    BEGIN
      INSERT INTO notification (notif_id, recipient_type, recipient_id, student_id, record_id, session_id,
                                category, title, message, destination, channel)
      VALUES (seq_notification.NEXTVAL, 'PARENT', v_parent_id, p_student_id, p_record_id, p_session_id,
              'ABSENT', 'Absent in period ' || v_period, 'Your ward ' || v_text, v_phone, 'SMS');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN NULL;
    END;
  END IF;
EXCEPTION
  WHEN NO_DATA_FOUND THEN NULL;
END notify_absence;
/

CREATE OR REPLACE PROCEDURE notify_period (p_session_id IN NUMBER) IS
  v_class   class.class_name%TYPE;
  v_advisor class.advisor_faculty_id%TYPE;
  v_date    attendance_session.session_date%TYPE;
  v_period  attendance_session.period_no%TYPE;
  v_code    subject.subject_code%TYPE;
  v_sname   subject.subject_name%TYPE;
  v_mail    faculty.email%TYPE;
  v_cnt     NUMBER;
  v_total   NUMBER;
  v_list    VARCHAR2(4000);
  v_title   VARCHAR2(120);
  v_msg     VARCHAR2(500);
  v_id      NUMBER;
  v_old     VARCHAR2(500);
BEGIN
  SELECT c.class_name, c.advisor_faculty_id, se.session_date, se.period_no, sb.subject_code, sb.subject_name
    INTO v_class, v_advisor, v_date, v_period, v_code, v_sname
    FROM attendance_session se
    JOIN class c   ON c.class_id = se.class_id
    JOIN subject sb ON sb.subject_id = se.subject_id
   WHERE se.session_id = p_session_id;
  IF v_advisor IS NULL THEN RETURN; END IF;
  SELECT email INTO v_mail FROM faculty WHERE faculty_id = v_advisor;

  SELECT COUNT(*) INTO v_total FROM attendance_record WHERE session_id = p_session_id;
  SELECT COUNT(*),
         LISTAGG(s.student_name || ' (#' || s.student_id || ')', ', ' ON OVERFLOW TRUNCATE '...') WITHIN GROUP (ORDER BY s.student_id)
    INTO v_cnt, v_list
    FROM attendance_record r JOIN student s ON s.student_id = r.student_id
   WHERE r.session_id = p_session_id AND r.status = 'ABSENT';

  v_title := v_class || ' - period ' || v_period || ' (' || v_code || '): ' || v_cnt || ' absent';
  IF v_cnt = 0 THEN
    v_msg := 'Everyone is present in period ' || v_period || ' - ' || v_code || ' ' || v_sname || ' on '
             || TO_CHAR(v_date, 'DD-Mon-YYYY') || ' (corrected).';
  ELSE
    v_msg := SUBSTR('Absentees in period ' || v_period || ' - ' || v_code || ' ' || v_sname || ' on '
             || TO_CHAR(v_date, 'DD-Mon-YYYY') || ' (' || v_cnt || ' of ' || v_total || '): ' || v_list, 1, 500);
  END IF;

  BEGIN
    SELECT notif_id, message INTO v_id, v_old FROM notification
     WHERE category = 'PERIOD' AND session_id = p_session_id;
    IF v_old <> v_msg THEN      -- the list changed after it was first sent: alert again with the corrected list
      UPDATE notification
         SET title = v_title, message = v_msg, status = 'PENDING', read_flag = 'N',
             sent_at = NULL, error_text = NULL, created_at = SYSTIMESTAMP
       WHERE notif_id = v_id;
    END IF;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    IF v_cnt > 0 THEN
      INSERT INTO notification (notif_id, recipient_type, recipient_id, student_id, record_id, session_id,
                                category, title, message, destination, channel)
      VALUES (seq_notification.NEXTVAL, 'ADVISOR', v_advisor, NULL, NULL, p_session_id,
              'PERIOD', v_title, v_msg, v_mail, 'EMAIL');
    END IF;
  END;
EXCEPTION
  WHEN NO_DATA_FOUND THEN NULL;
END notify_period;
/

CREATE OR REPLACE TRIGGER trg_attendance_notify
FOR INSERT OR UPDATE OF status ON attendance_record
COMPOUND TRIGGER
  TYPE t_flags IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
  g_sessions t_flags;

  AFTER EACH ROW IS
  BEGIN
    IF :NEW.status = 'ABSENT' THEN
      notify_absence(:NEW.record_id, :NEW.session_id, :NEW.student_id);   -- parent SMS
      g_sessions(:NEW.session_id) := 1;
    ELSIF UPDATING AND :OLD.status = 'ABSENT' THEN
      g_sessions(:NEW.session_id) := 1;                                    -- correction: refresh the digest
    END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
    k PLS_INTEGER;
  BEGIN
    k := g_sessions.FIRST;
    WHILE k IS NOT NULL LOOP
      notify_period(k);                                                    -- advisor digest, whole list at once
      k := g_sessions.NEXT(k);
    END LOOP;
    g_sessions.DELETE;
  END AFTER STATEMENT;
END trg_attendance_notify;
/

DECLARE
  v_count NUMBER := 0;
BEGIN
  FOR r IN (SELECT DISTINCT se.session_id
              FROM attendance_record ar
              JOIN attendance_session se ON se.session_id = ar.session_id
             WHERE ar.status = 'ABSENT'
               AND se.session_date = (SELECT MAX(session_date) FROM attendance_session)) LOOP
    notify_period(r.session_id);
    v_count := v_count + 1;
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('Advisor period digests queued: ' || v_count);
END;
/
COMMIT;

BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE digest_ready (done CHAR(1))';   -- marker: the digest upgrade finished
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

PROMPT === 19_portal_digest.sql finished ===
