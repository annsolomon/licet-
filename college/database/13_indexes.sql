-- =====================================================================
-- 13_indexes.sql                                    SYLLABUS: INDEXES
--
-- WITHOUT an index : Oracle reads the WHOLE table  (TABLE ACCESS FULL)
-- WITH an index    : Oracle jumps straight to the rows (INDEX RANGE/UNIQUE SCAN)
--
-- NOT every column gets an index (indexes cost space and slow INSERT/UPDATE):
--   * PRIMARY KEY and UNIQUE constraints ALREADY create an index
--     (student_id, subject_code, email ...) -> we do NOT create a duplicate.
--   * We index only columns used in frequent searches / joins that have no index yet.
--
-- Demo : EXPLAIN PLAN FOR SELECT * FROM student WHERE student_name = 'Rahul';
--        SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
-- =====================================================================
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- search box on the Students page
CREATE INDEX idx_student_name       ON student (student_name);
-- list students of a department (foreign key columns are NOT indexed automatically in Oracle)
CREATE INDEX idx_student_dept       ON student (dept_id);
-- student attendance lookup (the unique index starts with session_id, so it cannot help here)
CREATE INDEX idx_attrec_student     ON attendance_record (student_id);
-- audit screen shows the newest changes first
CREATE INDEX idx_audit_changed_date ON audit_log (changed_date);

PROMPT --- Indexes of the project (constraint indexes + the 4 above)
SELECT index_name, table_name, uniqueness FROM user_indexes ORDER BY table_name, index_name;

PROMPT --- WITH index (expect INDEX RANGE SCAN on IDX_STUDENT_NAME)
EXPLAIN PLAN FOR SELECT * FROM student WHERE student_name = 'Rahul';
SELECT plan_table_output FROM TABLE(DBMS_XPLAN.DISPLAY);

PROMPT --- WITHOUT index (hint forbids it: expect TABLE ACCESS FULL)
EXPLAIN PLAN FOR SELECT /*+ NO_INDEX(s idx_student_name) */ * FROM student s WHERE student_name = 'Rahul';
SELECT plan_table_output FROM TABLE(DBMS_XPLAN.DISPLAY);

PROMPT === 13_indexes.sql finished ===
