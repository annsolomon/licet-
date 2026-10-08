-- =====================================================================
-- 08_transactions.sql         SYLLABUS: TRANSACTIONS - COMMIT, ROLLBACK, SAVEPOINT
--
-- A transaction is a group of changes that succeeds or fails AS ONE UNIT.
--   COMMIT                   make the changes permanent
--   ROLLBACK                 undo everything since the last COMMIT
--   SAVEPOINT x              a bookmark inside the transaction
--   ROLLBACK TO SAVEPOINT x  undo only what happened after the bookmark
--
-- Scenario: faculty corrects Rahul's CS302 marks.
--   CT1 = 27 (original)   CT2 = 24 (original)
-- Run:  ./demo-sql.sh 08          (the script restores the original marks at the end)
-- Also available from the app: DBMS Lab -> "Transactions"
-- =====================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
SET LINESIZE 160
COLUMN assessment_code FORMAT A10

PROMPT ===== BEFORE =====
SELECT a.assessment_code, m.raw_marks FROM marks m JOIN assessment a ON a.assessment_id = m.assessment_id
 WHERE m.student_id = 101 AND m.subject_id = 2 AND a.assessment_code IN ('CT1','CT2') ORDER BY a.assessment_id;

PROMPT ===== STEP 1: UPDATE CT1 27 -> 28 (transaction starts automatically) =====
UPDATE marks SET raw_marks = 28
 WHERE student_id = 101 AND subject_id = 2 AND assessment_id = 2;

PROMPT ===== STEP 2: SAVEPOINT after the first update =====
SAVEPOINT after_ct1;

PROMPT ===== STEP 3: UPDATE CT2 24 -> 29 (second change) =====
UPDATE marks SET raw_marks = 29
 WHERE student_id = 101 AND subject_id = 2 AND assessment_id = 5;

SELECT a.assessment_code, m.raw_marks FROM marks m JOIN assessment a ON a.assessment_id = m.assessment_id
 WHERE m.student_id = 101 AND m.subject_id = 2 AND a.assessment_code IN ('CT1','CT2') ORDER BY a.assessment_id;
PROMPT (expected: CT1 = 28, CT2 = 29)

PROMPT ===== STEP 4: ROLLBACK TO SAVEPOINT - only the CT2 change is undone =====
ROLLBACK TO SAVEPOINT after_ct1;
SELECT a.assessment_code, m.raw_marks FROM marks m JOIN assessment a ON a.assessment_id = m.assessment_id
 WHERE m.student_id = 101 AND m.subject_id = 2 AND a.assessment_code IN ('CT1','CT2') ORDER BY a.assessment_id;
PROMPT (expected: CT1 = 28 (kept), CT2 = 24 (undone))

PROMPT ===== STEP 5: COMMIT - the CT1 change becomes permanent =====
COMMIT;

PROMPT ===== STEP 6: a mistake, then ROLLBACK - everything since COMMIT is undone =====
UPDATE marks SET raw_marks = 0 WHERE student_id = 101 AND subject_id = 2;
SELECT COUNT(*) AS rows_with_zero FROM marks WHERE student_id = 101 AND subject_id = 2 AND raw_marks = 0;
ROLLBACK;
SELECT COUNT(*) AS rows_with_zero_after_rollback FROM marks WHERE student_id = 101 AND subject_id = 2 AND raw_marks = 0;

PROMPT ===== The trigger recorded the committed change in AUDIT_LOG =====
SELECT table_name, operation, record_key, old_value, new_value, changed_by FROM audit_log ORDER BY audit_id DESC FETCH FIRST 3 ROWS ONLY;

PROMPT ===== RESTORE the original CT1 = 27 so the demo can be repeated =====
UPDATE marks SET raw_marks = 27 WHERE student_id = 101 AND subject_id = 2 AND assessment_id = 2;
COMMIT;

PROMPT === 08_transactions.sql finished ===
