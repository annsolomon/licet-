-- =====================================================================
-- 21_portal_internals.sql   (run as COLLEGE, after 20)
-- * Student login: app_user.student_id, role STUDENT  (s<student id> / student123)
-- * Internal-assessment marks (ASG1, CT1, CAT1, ASG2, CT2, CAT2) for every student and every subject of the
--   student's department. The original 10 students keep the marks they already have; nothing is overwritten.
--   The internal mark /40 is the existing function CALCULATE_INTERNAL_MARK (same formula as the main application).
-- Safe to run again.
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE app_user ADD (student_id NUMBER(10))';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -1430 THEN RAISE; END IF;   -- column already there
END;
/

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE app_user ADD CONSTRAINT fk_user_student FOREIGN KEY (student_id) REFERENCES student (student_id)';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE NOT IN (-2275, -2264) THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE app_user DROP CONSTRAINT chk_user_role';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -2443 THEN RAISE; END IF;
END;
/

ALTER TABLE app_user ADD CONSTRAINT chk_user_role CHECK (role_name IN ('ADMIN','FACULTY','HOD','ADVISOR','PARENT','STUDENT'));

DELETE FROM app_user WHERE role_name = 'STUDENT';
INSERT INTO app_user (user_id, username, password_hash, full_name, role_name, student_id)
SELECT seq_app_user.NEXTVAL, 's' || s.student_id,
       LOWER(RAWTOHEX(STANDARD_HASH('college-salt:student123', 'SHA256'))),
       s.student_name, 'STUDENT', s.student_id
  FROM student s;

-- raw internal marks for everybody who has none yet. Deterministic, but every student has a different level:
--   base 0.40 .. 0.70 by student id (a few students end up below the pass line of 20/40), Divya (104) is weak on purpose.
INSERT INTO marks (marks_id, student_id, subject_id, assessment_id, raw_marks)
SELECT seq_marks.NEXTVAL, x.student_id, x.subject_id, x.assessment_id, x.raw_marks
FROM (
  SELECT s.student_id, sb.subject_id, a.assessment_id,
         LEAST(a.max_marks, ROUND(a.max_marks * (CASE WHEN s.student_id = 104 THEN 0.36 ELSE 0.40 + MOD(s.student_id, 6) * 0.06 END
                                  + MOD(s.student_id * 7 + sb.subject_id * 3 + a.assessment_id * 5, 30) / 100))) AS raw_marks
    FROM student s
    JOIN subject sb    ON sb.dept_id = s.dept_id
    JOIN assessment a  ON a.assessment_code IN ('ASG1', 'CT1', 'CAT1', 'ASG2', 'CT2', 'CAT2')
   WHERE NOT EXISTS (SELECT 1 FROM marks m
                      WHERE m.student_id = s.student_id AND m.subject_id = sb.subject_id AND m.assessment_id = a.assessment_id)
) x;

COMMIT;

BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE internals_ready (done CHAR(1))';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

PROMPT === 21_portal_internals.sql finished ===
