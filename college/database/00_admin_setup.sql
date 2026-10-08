-- =====================================================================
-- 00_admin_setup.sql      (run as SYSTEM, inside the pluggable DB FREEPDB1)
-- Creates the application schema owner COLLEGE. Safe to run repeatedly.
-- Run by:  ./setup-db.sh   (you normally never run this by hand)
-- =====================================================================
SET DEFINE OFF
SET FEEDBACK ON
WHENEVER SQLERROR EXIT SQL.SQLCODE

DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM dba_users WHERE username = 'COLLEGE';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER college IDENTIFIED BY college123 QUOTA UNLIMITED ON USERS';
    DBMS_OUTPUT.PUT_LINE('User COLLEGE created.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('User COLLEGE already exists - reusing it.');
  END IF;
END;
/

GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE,
      CREATE PROCEDURE, CREATE TRIGGER, CREATE TYPE TO college;

EXIT
