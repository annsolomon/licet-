-- =====================================================================
-- 00_drop_all.sql          (run as COLLEGE)
-- Removes every object owned by COLLEGE so the scripts can be re-run.
-- Used by ./reset-demo.sh
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON

BEGIN
  -- views, code objects (triggers are dropped with their tables, but drop explicitly anyway)
  FOR o IN (SELECT object_name, object_type FROM user_objects
             WHERE object_type IN ('VIEW','TRIGGER','PROCEDURE','FUNCTION','PACKAGE')) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP ' || o.object_type || ' ' || o.object_name;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

  FOR t IN (SELECT table_name FROM user_tables) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
  END LOOP;

  FOR s IN (SELECT sequence_name FROM user_sequences) LOOP
    EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
  END LOOP;

  DBMS_OUTPUT.PUT_LINE('All COLLEGE objects dropped.');
END;
/

EXIT
