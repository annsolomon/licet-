-- =====================================================================
-- 02_constraints.sql      SYLLABUS: UNIQUE, FOREIGN KEY, CHECK, Referential
--                         Integrity, ALTER TABLE (DDL)
--
-- PRIMARY KEY and NOT NULL were declared in 01_tables.sql.
-- Demo: SELECT constraint_name, constraint_type, table_name FROM user_constraints;
-- =====================================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- ---------- UNIQUE ----------
ALTER TABLE app_user           ADD CONSTRAINT uq_app_user_name   UNIQUE (username);
ALTER TABLE department         ADD CONSTRAINT uq_dept_code       UNIQUE (dept_code);
ALTER TABLE department         ADD CONSTRAINT uq_dept_name       UNIQUE (dept_name);
ALTER TABLE faculty            ADD CONSTRAINT uq_faculty_code    UNIQUE (faculty_code);
ALTER TABLE faculty            ADD CONSTRAINT uq_faculty_email   UNIQUE (email);
ALTER TABLE subject            ADD CONSTRAINT uq_subject_code    UNIQUE (subject_code);
ALTER TABLE class              ADD CONSTRAINT uq_class_section   UNIQUE (dept_id, semester, section_name);
ALTER TABLE class_subject      ADD CONSTRAINT uq_class_subject   UNIQUE (class_id, subject_id);
ALTER TABLE student            ADD CONSTRAINT uq_student_email   UNIQUE (email);
ALTER TABLE employee           ADD CONSTRAINT uq_employee_email  UNIQUE (email);
ALTER TABLE payroll            ADD CONSTRAINT uq_payroll_month   UNIQUE (emp_id, pay_month);
ALTER TABLE attendance_session ADD CONSTRAINT uq_att_session     UNIQUE (class_id, subject_id, session_date, period_no);
ALTER TABLE attendance_record  ADD CONSTRAINT uq_att_record      UNIQUE (session_id, student_id);
ALTER TABLE assessment         ADD CONSTRAINT uq_assessment_code UNIQUE (assessment_code);
ALTER TABLE marks              ADD CONSTRAINT uq_marks_entry     UNIQUE (student_id, subject_id, assessment_id);
ALTER TABLE result             ADD CONSTRAINT uq_result_entry    UNIQUE (student_id, subject_id);

-- ---------- FOREIGN KEY (referential integrity) ----------
ALTER TABLE faculty            ADD CONSTRAINT fk_faculty_dept    FOREIGN KEY (dept_id)    REFERENCES department (dept_id);
ALTER TABLE subject            ADD CONSTRAINT fk_subject_dept    FOREIGN KEY (dept_id)    REFERENCES department (dept_id);
ALTER TABLE class              ADD CONSTRAINT fk_class_dept      FOREIGN KEY (dept_id)    REFERENCES department (dept_id);
ALTER TABLE class_subject      ADD CONSTRAINT fk_cs_class        FOREIGN KEY (class_id)   REFERENCES class (class_id);
ALTER TABLE class_subject      ADD CONSTRAINT fk_cs_subject      FOREIGN KEY (subject_id) REFERENCES subject (subject_id);
ALTER TABLE class_subject      ADD CONSTRAINT fk_cs_faculty      FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id);
ALTER TABLE student            ADD CONSTRAINT fk_student_dept    FOREIGN KEY (dept_id)    REFERENCES department (dept_id);
ALTER TABLE student            ADD CONSTRAINT fk_student_class   FOREIGN KEY (class_id)   REFERENCES class (class_id);
ALTER TABLE payroll            ADD CONSTRAINT fk_payroll_emp     FOREIGN KEY (emp_id)     REFERENCES employee (emp_id);
ALTER TABLE employee           ADD CONSTRAINT fk_employee_rule   FOREIGN KEY (designation) REFERENCES salary_rule (designation);
ALTER TABLE attendance_session ADD CONSTRAINT fk_attsess_class   FOREIGN KEY (class_id)   REFERENCES class (class_id);
ALTER TABLE attendance_session ADD CONSTRAINT fk_attsess_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id);
ALTER TABLE attendance_session ADD CONSTRAINT fk_attsess_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id);
-- Deleting a session deletes its attendance records (ON DELETE CASCADE)
ALTER TABLE attendance_record  ADD CONSTRAINT fk_attrec_session  FOREIGN KEY (session_id) REFERENCES attendance_session (session_id) ON DELETE CASCADE;
ALTER TABLE attendance_record  ADD CONSTRAINT fk_attrec_student  FOREIGN KEY (student_id) REFERENCES student (student_id);
ALTER TABLE marks              ADD CONSTRAINT fk_marks_student   FOREIGN KEY (student_id) REFERENCES student (student_id);
ALTER TABLE marks              ADD CONSTRAINT fk_marks_subject   FOREIGN KEY (subject_id) REFERENCES subject (subject_id);
ALTER TABLE marks              ADD CONSTRAINT fk_marks_assess    FOREIGN KEY (assessment_id) REFERENCES assessment (assessment_id);
ALTER TABLE result             ADD CONSTRAINT fk_result_student  FOREIGN KEY (student_id) REFERENCES student (student_id);
ALTER TABLE result             ADD CONSTRAINT fk_result_subject  FOREIGN KEY (subject_id) REFERENCES subject (subject_id);
ALTER TABLE result             ADD CONSTRAINT fk_result_grade    FOREIGN KEY (grade_code) REFERENCES grade (grade_code);

-- ---------- CHECK ----------
ALTER TABLE app_user           ADD CONSTRAINT chk_user_role      CHECK (role_name IN ('ADMIN','FACULTY'));
ALTER TABLE subject            ADD CONSTRAINT chk_subject_credit CHECK (credits BETWEEN 1 AND 6);
ALTER TABLE subject            ADD CONSTRAINT chk_subject_sem    CHECK (semester BETWEEN 1 AND 8);
ALTER TABLE class              ADD CONSTRAINT chk_class_sem      CHECK (semester BETWEEN 1 AND 8);
ALTER TABLE student            ADD CONSTRAINT chk_student_email  CHECK (email LIKE '%_@_%._%');
ALTER TABLE student            ADD CONSTRAINT chk_student_id     CHECK (student_id > 0);
ALTER TABLE employee           ADD CONSTRAINT chk_emp_desig      CHECK (designation IN ('PROGRAMMER','ASSISTANT_PROFESSOR','ASSOCIATE_PROFESSOR','PROFESSOR'));
ALTER TABLE employee           ADD CONSTRAINT chk_emp_basic      CHECK (basic_salary > 0);
ALTER TABLE payroll            ADD CONSTRAINT chk_payroll_month  CHECK (REGEXP_LIKE(pay_month, '^[0-9]{4}-(0[1-9]|1[0-2])$'));
ALTER TABLE payroll            ADD CONSTRAINT chk_payroll_net    CHECK (net = gross - pf AND gross = basic + hra + da);
ALTER TABLE attendance_session ADD CONSTRAINT chk_att_period     CHECK (period_no BETWEEN 1 AND 8);
ALTER TABLE attendance_record  ADD CONSTRAINT chk_att_status     CHECK (status IN ('PRESENT','ABSENT','OD','MEDICAL','OTHER'));
ALTER TABLE assessment         ADD CONSTRAINT chk_assess_max     CHECK (max_marks > 0);
ALTER TABLE marks              ADD CONSTRAINT chk_marks_nonneg   CHECK (raw_marks >= 0);
ALTER TABLE grade              ADD CONSTRAINT chk_grade_range    CHECK (min_mark <= max_mark);
ALTER TABLE grade              ADD CONSTRAINT chk_grade_pass     CHECK (pass_flag IN ('Y','N'));
ALTER TABLE result             ADD CONSTRAINT chk_result_pf      CHECK (pass_fail IN ('PASS','FAIL'));

PROMPT === 02_constraints.sql finished ===
