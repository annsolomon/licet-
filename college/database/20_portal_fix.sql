-- =====================================================================
-- 20_portal_fix.sql   (run as COLLEGE, after 19)
-- Makes sure EVERY class has 6 subjects, a full 5-day x 8-period timetable, and 8 periods of attendance
-- for 10 working days (25 Sep - 8 Oct 2026). Safe to run again.
--   1. tops every class up to 6 subjects (teacher: a faculty member of the department)
--   2. rebuilds the timetable
--   3. rebuilds the attendance of 25 Sep onwards (trigger switched off meanwhile, so nothing is sent for history)
--   4. queues the alerts of the latest day (parent SMS per absence, one advisor digest per period)
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
  v_have NUMBER;
  v_cnt  NUMBER;
BEGIN
  FOR c IN (SELECT class_id, dept_id FROM class) LOOP
    SELECT COUNT(*) INTO v_have FROM class_subject WHERE class_id = c.class_id;
    SELECT COUNT(*) INTO v_cnt FROM faculty WHERE dept_id = c.dept_id;
    FOR s IN (SELECT subject_id FROM subject sb
               WHERE sb.dept_id = c.dept_id
                 AND NOT EXISTS (SELECT 1 FROM class_subject x WHERE x.class_id = c.class_id AND x.subject_id = sb.subject_id)
               ORDER BY sb.subject_id) LOOP
      EXIT WHEN v_have >= 6 OR v_cnt = 0;
      INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id)
      VALUES (seq_class_subject.NEXTVAL, c.class_id, s.subject_id,
              (SELECT faculty_id FROM (SELECT faculty_id, ROW_NUMBER() OVER (ORDER BY faculty_id) rn FROM faculty WHERE dept_id = c.dept_id)
                WHERE rn = MOD(s.subject_id, v_cnt) + 1));
      v_have := v_have + 1;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('class ' || c.class_id || ': ' || v_have || ' subjects');
  END LOOP;
END;
/

ALTER TRIGGER trg_attendance_notify DISABLE;

DELETE FROM notification;
-- the attendance records go with the sessions (ON DELETE CASCADE)
DELETE FROM attendance_session WHERE session_date >= DATE '2026-09-25';
DELETE FROM timetable;

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

DELETE FROM audit_log;
COMMIT;

ALTER TRIGGER trg_attendance_notify ENABLE;

DECLARE
  v_p NUMBER := 0;
  v_s NUMBER := 0;
BEGIN
  FOR r IN (SELECT ar.record_id, ar.session_id, ar.student_id
              FROM attendance_record ar JOIN attendance_session se ON se.session_id = ar.session_id
             WHERE ar.status = 'ABSENT' AND se.session_date = (SELECT MAX(session_date) FROM attendance_session)) LOOP
    notify_absence(r.record_id, r.session_id, r.student_id);
    v_p := v_p + 1;
  END LOOP;
  FOR r IN (SELECT DISTINCT se.session_id
              FROM attendance_record ar JOIN attendance_session se ON se.session_id = ar.session_id
             WHERE ar.status = 'ABSENT' AND se.session_date = (SELECT MAX(session_date) FROM attendance_session)) LOOP
    notify_period(r.session_id);
    v_s := v_s + 1;
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('Latest day queued: ' || v_p || ' parent SMS, ' || v_s || ' advisor period digests');
END;
/
COMMIT;

BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE timetable_fixed (done CHAR(1))';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

PROMPT === 20_portal_fix.sql finished ===
