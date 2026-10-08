-- =====================================================================
-- 14_users_roles.sql                         SYLLABUS: DCL  (GRANT, REVOKE)
--
-- RUN AS SYSTEM (creating users and roles needs DBA rights). ./setup-db.sh does it.
--
-- Why permissions matter: a faculty member must be able to mark attendance but
-- must NOT be able to delete students; a student may only SEE his own results.
-- A ROLE is a named bundle of privileges; users receive roles.
--
-- Demo (as SYSTEM):
--   SELECT grantee, table_name, privilege FROM dba_tab_privs WHERE owner = 'COLLEGE'
--    AND grantee LIKE 'COLLEGE_%';
--   REVOKE UPDATE ON college.marks FROM college_faculty_role;   -- take a privilege away
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE

-- helper: run a statement and ignore "already exists" errors so the script is repeatable
DECLARE
  PROCEDURE run_ignore (p_sql VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE p_sql;
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE NOT IN (-1920, -1921, -1918) THEN RAISE; END IF;   -- user/role exists (or missing on drop)
  END;
BEGIN
  run_ignore('CREATE ROLE college_faculty_role');
  run_ignore('CREATE ROLE college_student_role');
  run_ignore('CREATE USER college_faculty IDENTIFIED BY faculty123');
  run_ignore('CREATE USER college_student IDENTIFIED BY student123');
END;
/

GRANT CREATE SESSION TO college_faculty_role;
GRANT CREATE SESSION TO college_student_role;
GRANT college_faculty_role TO college_faculty;
GRANT college_student_role TO college_student;

-- FACULTY: read academic data, record attendance and marks
GRANT SELECT ON college.student            TO college_faculty_role;
GRANT SELECT ON college.subject            TO college_faculty_role;
GRANT SELECT ON college.attendance_session TO college_faculty_role;
GRANT SELECT, INSERT, UPDATE ON college.attendance_record TO college_faculty_role;
GRANT SELECT, INSERT, UPDATE ON college.marks             TO college_faculty_role;
GRANT SELECT ON college.student_attendance_view           TO college_faculty_role;

-- STUDENT: read-only access to the result and attendance VIEWS only (not to the tables)
GRANT SELECT ON college.student_result_view     TO college_student_role;
GRANT SELECT ON college.student_attendance_view TO college_student_role;

-- REVOKE demo: faculty may enter marks but not delete attendance; remove UPDATE on records
REVOKE UPDATE ON college.attendance_record FROM college_faculty_role;

PROMPT --- Privileges now held by the two roles
SELECT grantee, table_name, privilege
  FROM dba_tab_privs
 WHERE owner = 'COLLEGE' AND grantee IN ('COLLEGE_FACULTY_ROLE', 'COLLEGE_STUDENT_ROLE')
 ORDER BY grantee, table_name, privilege;

PROMPT === 14_users_roles.sql finished ===
EXIT
