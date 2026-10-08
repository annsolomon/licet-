-- =====================================================================
-- 01_tables.sql                                   SYLLABUS: DDL (CREATE TABLE)
-- Creates all tables with column types, NOT NULL and PRIMARY KEY.
-- Other constraints (UNIQUE / FOREIGN KEY / CHECK) are added in 02 with
-- ALTER TABLE so that both CREATE and ALTER (DDL) are demonstrated.
--
-- Project use : every module reads/writes these tables through JdbcTemplate.
-- Demo        : DESC student;   SELECT table_name FROM user_tables;
-- =====================================================================
SET DEFINE OFF
SET FEEDBACK ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- Login accounts for the React app
CREATE TABLE app_user (
  user_id        NUMBER(10)    NOT NULL,
  username       VARCHAR2(50)  NOT NULL,
  password_hash  VARCHAR2(128) NOT NULL,   -- SHA-256 hex, never plain text
  full_name      VARCHAR2(100) NOT NULL,
  role_name      VARCHAR2(20)  NOT NULL,
  CONSTRAINT pk_app_user PRIMARY KEY (user_id)
);

CREATE TABLE department (
  dept_id    NUMBER(10)    NOT NULL,
  dept_code  VARCHAR2(10)  NOT NULL,
  dept_name  VARCHAR2(100) NOT NULL,
  CONSTRAINT pk_department PRIMARY KEY (dept_id)
);

CREATE TABLE faculty (
  faculty_id    NUMBER(10)    NOT NULL,
  faculty_code  VARCHAR2(10)  NOT NULL,
  faculty_name  VARCHAR2(100) NOT NULL,
  email         VARCHAR2(100) NOT NULL,
  designation   VARCHAR2(40),
  dept_id       NUMBER(10)    NOT NULL,
  CONSTRAINT pk_faculty PRIMARY KEY (faculty_id)
);

CREATE TABLE subject (
  subject_id    NUMBER(10)    NOT NULL,
  subject_code  VARCHAR2(10)  NOT NULL,
  subject_name  VARCHAR2(100) NOT NULL,
  credits       NUMBER(2)     NOT NULL,
  semester      NUMBER(2)     NOT NULL,
  dept_id       NUMBER(10)    NOT NULL,
  CONSTRAINT pk_subject PRIMARY KEY (subject_id)
);

CREATE TABLE class (
  class_id       NUMBER(10)   NOT NULL,
  class_name     VARCHAR2(50) NOT NULL,
  dept_id        NUMBER(10)   NOT NULL,
  semester       NUMBER(2)    NOT NULL,
  section_name   VARCHAR2(5)  NOT NULL,
  academic_year  VARCHAR2(9),
  CONSTRAINT pk_class PRIMARY KEY (class_id)
);

-- Which faculty teaches which subject to which class
CREATE TABLE class_subject (
  cs_id       NUMBER(10) NOT NULL,
  class_id    NUMBER(10) NOT NULL,
  subject_id  NUMBER(10) NOT NULL,
  faculty_id  NUMBER(10) NOT NULL,
  CONSTRAINT pk_class_subject PRIMARY KEY (cs_id)
);

CREATE TABLE student (
  student_id    NUMBER(10)    NOT NULL,
  student_name  VARCHAR2(100) NOT NULL,
  email         VARCHAR2(100) NOT NULL,
  phone         VARCHAR2(15),
  dept_id       NUMBER(10)    NOT NULL,
  class_id      NUMBER(10)    NOT NULL,
  joined_year   NUMBER(4),
  CONSTRAINT pk_student PRIMARY KEY (student_id)
);

CREATE TABLE employee (
  emp_id        NUMBER(10)    NOT NULL,
  emp_name      VARCHAR2(100) NOT NULL,
  email         VARCHAR2(100) NOT NULL,
  designation   VARCHAR2(30)  NOT NULL,
  basic_salary  NUMBER(10,2)  NOT NULL,
  CONSTRAINT pk_employee PRIMARY KEY (emp_id)
);

-- Configurable salary percentages per designation
CREATE TABLE salary_rule (
  designation  VARCHAR2(30) NOT NULL,
  hra_pct      NUMBER(5,2)  NOT NULL,
  da_pct       NUMBER(5,2)  NOT NULL,
  pf_pct       NUMBER(5,2)  NOT NULL,
  CONSTRAINT pk_salary_rule PRIMARY KEY (designation)
);

CREATE TABLE payroll (
  payroll_id    NUMBER(10)   NOT NULL,
  emp_id        NUMBER(10)   NOT NULL,
  pay_month     VARCHAR2(7)  NOT NULL,      -- 'YYYY-MM'
  basic         NUMBER(10,2) NOT NULL,
  hra           NUMBER(10,2) NOT NULL,
  da            NUMBER(10,2) NOT NULL,
  pf            NUMBER(10,2) NOT NULL,
  gross         NUMBER(10,2) NOT NULL,
  net           NUMBER(10,2) NOT NULL,
  generated_on  DATE DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_payroll PRIMARY KEY (payroll_id)
);

-- One row per class period in which attendance was taken
CREATE TABLE attendance_session (
  session_id    NUMBER(10) NOT NULL,
  class_id      NUMBER(10) NOT NULL,
  subject_id    NUMBER(10) NOT NULL,
  faculty_id    NUMBER(10) NOT NULL,
  session_date  DATE       NOT NULL,
  period_no     NUMBER(1)  NOT NULL,
  CONSTRAINT pk_attendance_session PRIMARY KEY (session_id)
);

-- One row per student per session
CREATE TABLE attendance_record (
  record_id   NUMBER(10)  NOT NULL,
  session_id  NUMBER(10)  NOT NULL,
  student_id  NUMBER(10)  NOT NULL,
  status      VARCHAR2(10) NOT NULL,
  CONSTRAINT pk_attendance_record PRIMARY KEY (record_id)
);

-- Definition of each assessment component and its maximum raw mark
CREATE TABLE assessment (
  assessment_id    NUMBER(10)    NOT NULL,
  assessment_code  VARCHAR2(10)  NOT NULL,
  assessment_name  VARCHAR2(60)  NOT NULL,
  max_marks        NUMBER(5,2)   NOT NULL,
  part_no          NUMBER(1)     NOT NULL,   -- 1 = part 1, 2 = part 2, 3 = semester exam
  CONSTRAINT pk_assessment PRIMARY KEY (assessment_id)
);

-- RAW marks only. Converted marks are NEVER stored here.
CREATE TABLE marks (
  marks_id       NUMBER(10)   NOT NULL,
  student_id     NUMBER(10)   NOT NULL,
  subject_id     NUMBER(10)   NOT NULL,
  assessment_id  NUMBER(10)   NOT NULL,
  raw_marks      NUMBER(5,2)  NOT NULL,
  CONSTRAINT pk_marks PRIMARY KEY (marks_id)
);

-- Configurable grade scale
CREATE TABLE grade (
  grade_code   VARCHAR2(3)  NOT NULL,
  min_mark     NUMBER(5,2)  NOT NULL,
  max_mark     NUMBER(5,2)  NOT NULL,
  grade_point  NUMBER(3,1)  NOT NULL,
  pass_flag    CHAR(1)      NOT NULL,
  CONSTRAINT pk_grade PRIMARY KEY (grade_code)
);

-- Calculated result per student per subject
CREATE TABLE result (
  result_id       NUMBER(10)   NOT NULL,
  student_id      NUMBER(10)   NOT NULL,
  subject_id      NUMBER(10)   NOT NULL,
  semester        NUMBER(2)    NOT NULL,
  internal_mark   NUMBER(5,2)  NOT NULL,   -- out of 40
  semester_mark   NUMBER(5,2)  NOT NULL,   -- out of 60
  final_mark      NUMBER(5,2)  NOT NULL,   -- out of 100
  grade_code      VARCHAR2(3)  NOT NULL,
  grade_point     NUMBER(3,1)  NOT NULL,
  credits         NUMBER(2)    NOT NULL,
  pass_fail       VARCHAR2(4)  NOT NULL,
  calculated_on   DATE DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_result PRIMARY KEY (result_id)
);

CREATE TABLE audit_log (
  audit_id      NUMBER(12)    NOT NULL,
  table_name    VARCHAR2(30)  NOT NULL,
  operation     VARCHAR2(10)  NOT NULL,
  student_id    NUMBER(10),
  record_key    VARCHAR2(100),
  old_value     VARCHAR2(4000),
  new_value     VARCHAR2(4000),
  changed_by    VARCHAR2(100),
  changed_date  TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_audit_log PRIMARY KEY (audit_id)
);

PROMPT === 01_tables.sql finished: 17 tables created ===
