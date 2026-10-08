-- =====================================================================
-- 03_sequences.sql                          SYLLABUS: DDL (CREATE SEQUENCE)
-- A sequence hands out unique numbers (used for primary keys).
-- Project use : Java asks "SELECT seq_student.NEXTVAL FROM dual" and uses the
--               number as the new STUDENT_ID.
-- Demo        : SELECT seq_student.NEXTVAL FROM dual;
-- Sample data uses fixed IDs for master tables (department, faculty ...), so
-- those sequences START ABOVE the sample IDs.
-- =====================================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

CREATE SEQUENCE seq_app_user      START WITH 3    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_department    START WITH 5    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_faculty       START WITH 7    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_subject       START WITH 9    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_class         START WITH 5    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_class_subject START WITH 7    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_student       START WITH 111  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_employee      START WITH 1006 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_payroll       START WITH 1    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_att_session   START WITH 1    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_att_record    START WITH 1    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_marks         START WITH 1    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_result        START WITH 1    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_audit         START WITH 1    INCREMENT BY 1 NOCACHE;

PROMPT === 03_sequences.sql finished ===
