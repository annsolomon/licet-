-- =====================================================================
-- 16_portal_schema.sql   (run as COLLEGE, after 01-15)
-- Adds what the HOD / Parent / Class-advisor portals need:
--   PARENT, PERIOD_SLOT, TIMETABLE, NOTIFICATION tables
--   student.parent_id, department.hod_faculty_id, class.advisor_faculty_id
--   app_user.faculty_id / parent_id and the new roles HOD, ADVISOR, PARENT
--   procedure NOTIFY_ABSENCE (creates the parent + advisor notifications)
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

CREATE TABLE parent (
  parent_id    NUMBER(10)    NOT NULL,
  parent_name  VARCHAR2(100) NOT NULL,
  relation     VARCHAR2(10)  NOT NULL,
  phone        VARCHAR2(15)  NOT NULL,
  email        VARCHAR2(100),
  CONSTRAINT pk_parent PRIMARY KEY (parent_id),
  CONSTRAINT chk_parent_relation CHECK (relation IN ('FATHER','MOTHER','GUARDIAN'))
);

ALTER TABLE student ADD (parent_id NUMBER(10));
ALTER TABLE student ADD CONSTRAINT fk_student_parent FOREIGN KEY (parent_id) REFERENCES parent (parent_id);

ALTER TABLE department ADD (hod_faculty_id NUMBER(10));
ALTER TABLE department ADD CONSTRAINT fk_dept_hod FOREIGN KEY (hod_faculty_id) REFERENCES faculty (faculty_id);

ALTER TABLE class ADD (advisor_faculty_id NUMBER(10));
ALTER TABLE class ADD CONSTRAINT fk_class_advisor FOREIGN KEY (advisor_faculty_id) REFERENCES faculty (faculty_id);

ALTER TABLE app_user ADD (faculty_id NUMBER(10), parent_id NUMBER(10));
ALTER TABLE app_user ADD CONSTRAINT fk_user_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id);
ALTER TABLE app_user ADD CONSTRAINT fk_user_parent  FOREIGN KEY (parent_id)  REFERENCES parent (parent_id);
ALTER TABLE app_user DROP CONSTRAINT chk_user_role;
ALTER TABLE app_user ADD CONSTRAINT chk_user_role CHECK (role_name IN ('ADMIN','FACULTY','HOD','ADVISOR','PARENT'));

CREATE TABLE period_slot (
  period_no   NUMBER(1)    NOT NULL,
  start_time  VARCHAR2(5)  NOT NULL,
  end_time    VARCHAR2(5)  NOT NULL,
  CONSTRAINT pk_period_slot PRIMARY KEY (period_no),
  CONSTRAINT chk_slot_period CHECK (period_no BETWEEN 1 AND 8)
);

CREATE TABLE timetable (
  tt_id        NUMBER(10) NOT NULL,
  class_id     NUMBER(10) NOT NULL,
  day_of_week  NUMBER(1)  NOT NULL,
  period_no    NUMBER(1)  NOT NULL,
  subject_id   NUMBER(10) NOT NULL,
  faculty_id   NUMBER(10) NOT NULL,
  CONSTRAINT pk_timetable PRIMARY KEY (tt_id),
  CONSTRAINT uq_timetable_slot UNIQUE (class_id, day_of_week, period_no),
  CONSTRAINT chk_tt_day    CHECK (day_of_week BETWEEN 1 AND 5),
  CONSTRAINT chk_tt_period CHECK (period_no BETWEEN 1 AND 8),
  CONSTRAINT fk_tt_class   FOREIGN KEY (class_id)   REFERENCES class (class_id),
  CONSTRAINT fk_tt_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id),
  CONSTRAINT fk_tt_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id)
);

CREATE TABLE notification (
  notif_id        NUMBER(12)    NOT NULL,
  recipient_type  VARCHAR2(10)  NOT NULL,
  recipient_id    NUMBER(10)    NOT NULL,
  student_id      NUMBER(10)    NOT NULL,
  record_id       NUMBER(10),
  session_id      NUMBER(10),
  category        VARCHAR2(20)  DEFAULT 'ABSENT' NOT NULL,
  title           VARCHAR2(120) NOT NULL,
  message         VARCHAR2(500) NOT NULL,
  destination     VARCHAR2(100),
  channel         VARCHAR2(10)  DEFAULT 'SMS' NOT NULL,
  status          VARCHAR2(10)  DEFAULT 'PENDING' NOT NULL,
  read_flag       CHAR(1)       DEFAULT 'N' NOT NULL,
  created_at      TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  sent_at         TIMESTAMP,
  error_text      VARCHAR2(200),
  CONSTRAINT pk_notification PRIMARY KEY (notif_id),
  CONSTRAINT uq_notif_record UNIQUE (record_id, recipient_type),
  CONSTRAINT chk_notif_type   CHECK (recipient_type IN ('PARENT','ADVISOR')),
  CONSTRAINT chk_notif_status CHECK (status IN ('PENDING','SENT','FAILED')),
  CONSTRAINT chk_notif_read   CHECK (read_flag IN ('Y','N')),
  CONSTRAINT fk_notif_student FOREIGN KEY (student_id) REFERENCES student (student_id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_recipient ON notification (recipient_type, recipient_id, created_at);
CREATE INDEX idx_notif_status    ON notification (status);
CREATE INDEX idx_att_session_date ON attendance_session (session_date, class_id);

CREATE SEQUENCE seq_parent       START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_timetable    START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_notification START WITH 1 INCREMENT BY 1 NOCACHE;

-- ---------------------------------------------------------------------
-- NOTIFY_ABSENCE: called by the trigger for every ABSENT mark.
-- It receives the row values (a row trigger must not read ATTENDANCE_RECORD itself: ORA-04091).
-- One PARENT notification (SMS) and one ADVISOR notification (e-mail) per absent period.
-- The unique key (record_id, recipient_type) makes repeated marking harmless.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE notify_absence (
  p_record_id  IN NUMBER,
  p_session_id IN NUMBER,
  p_student_id IN NUMBER
) IS
  v_name      student.student_name%TYPE;
  v_parent_id student.parent_id%TYPE;
  v_class     class.class_name%TYPE;
  v_advisor   class.advisor_faculty_id%TYPE;
  v_date      attendance_session.session_date%TYPE;
  v_period    attendance_session.period_no%TYPE;
  v_code      subject.subject_code%TYPE;
  v_sname     subject.subject_name%TYPE;
  v_phone     parent.phone%TYPE;
  v_mail      faculty.email%TYPE;
  v_text      VARCHAR2(300);
BEGIN
  SELECT s.student_name, s.parent_id, c.class_name, c.advisor_faculty_id,
         se.session_date, se.period_no, sb.subject_code, sb.subject_name
    INTO v_name, v_parent_id, v_class, v_advisor, v_date, v_period, v_code, v_sname
    FROM student s
    JOIN class c               ON c.class_id = s.class_id
    JOIN attendance_session se ON se.session_id = p_session_id
    JOIN subject sb            ON sb.subject_id = se.subject_id
   WHERE s.student_id = p_student_id;

  v_text := v_name || ' (' || p_student_id || ') was ABSENT in period ' || v_period || ' - '
            || v_code || ' ' || v_sname || ' on ' || TO_CHAR(v_date, 'DD-Mon-YYYY') || '.';

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

  IF v_advisor IS NOT NULL THEN
    SELECT email INTO v_mail FROM faculty WHERE faculty_id = v_advisor;
    BEGIN
      INSERT INTO notification (notif_id, recipient_type, recipient_id, student_id, record_id, session_id,
                                category, title, message, destination, channel)
      VALUES (seq_notification.NEXTVAL, 'ADVISOR', v_advisor, p_student_id, p_record_id, p_session_id,
              'ABSENT', v_class || ': absent in period ' || v_period, v_class || ' - ' || v_text, v_mail, 'EMAIL');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN NULL;
    END;
  END IF;
EXCEPTION
  WHEN NO_DATA_FOUND THEN NULL;
END notify_absence;
/

PROMPT === 16_portal_schema.sql finished ===
