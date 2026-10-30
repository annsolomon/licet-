-- =====================================================================
-- 22_absentee_list.sql   (run as COLLEGE, after 21)
-- The advisor's period alert now lists the absentees ONE BY ONE (one numbered line per student)
-- instead of one comma-joined group:
--
--     CSE-A - period 3 (CS302): 3 absent
--     Absentees in period 3 - CS302 DBMS on 08-Oct-2026 (3 of 60):
--     1. Divya (#104)
--     2. Karthik (#119)
--     3. Meena (#152)
--
-- MESSAGE is widened to 4000 so long lists are not cut off.
-- Also adds view ABSENTEE_LIST_VIEW: one row per absent student per period.
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

ALTER TABLE notification MODIFY (message VARCHAR2(4000));

CREATE OR REPLACE VIEW absentee_list_view AS
SELECT se.session_date, se.period_no, se.session_id, c.class_id, c.class_name,
       sb.subject_code, sb.subject_name, f.faculty_name,
       ROW_NUMBER() OVER (PARTITION BY se.session_id ORDER BY s.student_id) AS sno,
       s.student_id, s.student_name
  FROM attendance_record  ar
  JOIN attendance_session se ON se.session_id = ar.session_id
  JOIN class              c  ON c.class_id    = se.class_id
  JOIN subject            sb ON sb.subject_id = se.subject_id
  JOIN faculty            f  ON f.faculty_id  = se.faculty_id
  JOIN student            s  ON s.student_id  = ar.student_id
 WHERE ar.status = 'ABSENT';

CREATE OR REPLACE PROCEDURE notify_period (p_session_id IN NUMBER) IS
  v_class   class.class_name%TYPE;
  v_advisor class.advisor_faculty_id%TYPE;
  v_date    attendance_session.session_date%TYPE;
  v_period  attendance_session.period_no%TYPE;
  v_code    subject.subject_code%TYPE;
  v_sname   subject.subject_name%TYPE;
  v_mail    faculty.email%TYPE;
  v_cnt     NUMBER := 0;
  v_total   NUMBER;
  v_list    VARCHAR2(3600);
  v_line    VARCHAR2(200);
  v_title   VARCHAR2(120);
  v_msg     VARCHAR2(4000);
  v_id      NUMBER;
  v_old     VARCHAR2(4000);
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

  -- one line per absentee (cursor FOR loop, no LISTAGG grouping)
  FOR r IN (SELECT s.student_id, s.student_name
              FROM attendance_record ar JOIN student s ON s.student_id = ar.student_id
             WHERE ar.session_id = p_session_id AND ar.status = 'ABSENT'
             ORDER BY s.student_id) LOOP
    v_cnt  := v_cnt + 1;
    v_line := v_cnt || '. ' || r.student_name || ' (#' || r.student_id || ')';
    IF NVL(LENGTH(v_list), 0) + LENGTH(v_line) + 1 <= 3500 THEN
      v_list := v_list || CHR(10) || v_line;
    END IF;
  END LOOP;

  v_title := v_class || ' - period ' || v_period || ' (' || v_code || '): ' || v_cnt || ' absent';
  IF v_cnt = 0 THEN
    v_msg := 'Everyone is present in period ' || v_period || ' - ' || v_code || ' ' || v_sname || ' on '
             || TO_CHAR(v_date, 'DD-Mon-YYYY') || ' (corrected).';
  ELSE
    v_msg := 'Absentees in period ' || v_period || ' - ' || v_code || ' ' || v_sname || ' on '
             || TO_CHAR(v_date, 'DD-Mon-YYYY') || ' (' || v_cnt || ' of ' || v_total || '):' || v_list;
  END IF;

  BEGIN
    SELECT notif_id, message INTO v_id, v_old FROM notification
     WHERE category = 'PERIOD' AND session_id = p_session_id;
    IF v_old <> v_msg THEN
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

-- rebuild the pending advisor alerts of the latest day in the new one-by-one format
BEGIN
  FOR r IN (SELECT DISTINCT se.session_id
              FROM attendance_record ar JOIN attendance_session se ON se.session_id = ar.session_id
             WHERE ar.status = 'ABSENT' AND se.session_date = (SELECT MAX(session_date) FROM attendance_session)) LOOP
    notify_period(r.session_id);
  END LOOP;
END;
/
COMMIT;

PROMPT === 22_absentee_list.sql finished ===
