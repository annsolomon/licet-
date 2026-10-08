package com.college.service;

import com.college.dto.DemoStepResult;
import com.college.dto.DemoTopic;
import com.college.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DBMS LAB - the 32 DBMS syllabus topics, each with real SQL/PL-SQL that runs on the real project tables.
 *
 * Every demo runs on its OWN connection with auto-commit OFF, and ends with ROLLBACK, so the sample data is
 * never changed (the COMMIT topic only commits a no-op update). The SQL is fixed text in this class - the
 * browser can only choose the topic id, never send SQL (no SQL injection possible).
 *
 * Step kinds:  Q query | D DML | L DDL | C control (COMMIT/ROLLBACK/SAVEPOINT) | P PL/SQL block
 *              E statement that MUST fail (shows the Oracle error) | I silent clean-up (errors ignored)
 */
@Service
public class DbmsDemoService {

    private record Step(char kind, String sql, String note) { }

    private record Topic(DemoTopic info, List<Step> steps) { }

    private static final int MAX_ROWS = 40;
    private static final String SUBJECT = "(SELECT subject_id FROM subject WHERE subject_code = 'CS302')";

    private final DataSource dataSource;
    private final List<Topic> topics = new ArrayList<>();

    public DbmsDemoService(DataSource dataSource) {
        this.dataSource = dataSource;
        build();
    }

    // ------------------------------------------------------------------ public API
    public List<DemoTopic> catalog() {
        return topics.stream().map(Topic::info).toList();
    }

    public DemoTopic topic(int number) {
        return find(number).info();
    }

    /** Runs every statement of one topic and returns what Oracle answered. */
    public List<DemoStepResult> run(int number) {
        Topic t = find(number);
        List<DemoStepResult> out = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                enableOutput(con);
                for (Step s : t.steps()) {
                    DemoStepResult r = execute(con, s);
                    if (r != null) {
                        out.add(r);
                    }
                }
            } finally {
                con.rollback();          // leaves the sample data untouched
            }
        } catch (SQLException e) {
            out.add(new DemoStepResult("(connection)", "ERROR", "", List.of(), List.of(), oraMessage(e), true));
        }
        return out;
    }

    // ------------------------------------------------------------------ execution
    private DemoStepResult execute(Connection con, Step s) {
        String kind = switch (s.kind()) {
            case 'Q' -> "QUERY"; case 'D' -> "DML"; case 'L' -> "DDL"; case 'C' -> "CONTROL";
            case 'P' -> "PLSQL"; case 'E' -> "EXPECT_ERROR"; default -> "SETUP";
        };
        try {
            if (s.kind() == 'C') {
                String word = s.sql().trim().toUpperCase();
                if (word.equals("COMMIT")) {
                    con.commit();
                } else if (word.equals("ROLLBACK")) {
                    con.rollback();
                } else {
                    try (Statement st = con.createStatement()) { st.execute(s.sql()); }
                }
                return new DemoStepResult(s.sql(), kind, s.note(), List.of(), List.of(), "OK", false);
            }
            try (Statement st = con.createStatement()) {
                boolean hasRows = st.execute(s.sql());
                if (s.kind() == 'I') {
                    return null;
                }
                if (hasRows) {
                    return rows(s, kind, st.getResultSet());
                }
                String msg = s.kind() == 'P' ? plsqlOutput(con) : st.getUpdateCount() >= 0
                        ? st.getUpdateCount() + " row(s) affected" : "OK";
                if (s.kind() == 'E') {
                    return new DemoStepResult(s.sql(), kind, s.note(), List.of(), List.of(),
                            "UNEXPECTED: the statement succeeded", true);
                }
                return new DemoStepResult(s.sql(), kind, s.note(), List.of(), List.of(), msg, false);
            }
        } catch (SQLException e) {
            if (s.kind() == 'I') {
                return null;
            }
            boolean expected = s.kind() == 'E';
            return new DemoStepResult(s.sql(), kind, s.note(), List.of(), List.of(),
                    (expected ? "Expected Oracle error -> " : "") + oraMessage(e), !expected);
        }
    }

    private DemoStepResult rows(Step s, String kind, ResultSet rs) throws SQLException {
        try (rs) {
            ResultSetMetaData md = rs.getMetaData();
            List<String> cols = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                cols.add(md.getColumnLabel(i));
            }
            List<List<String>> data = new ArrayList<>();
            while (rs.next() && data.size() < MAX_ROWS) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= cols.size(); i++) {
                    String v = rs.getString(i);
                    row.add(v == null ? "NULL" : v);
                }
                data.add(row);
            }
            return new DemoStepResult(s.sql(), kind, s.note(), cols, data, data.size() + " row(s)", false);
        }
    }

    private static void enableOutput(Connection con) throws SQLException {
        try (CallableStatement cs = con.prepareCall("{call DBMS_OUTPUT.ENABLE(1000000)}")) {
            cs.execute();
        }
    }

    /** Reads what PL/SQL printed with DBMS_OUTPUT.PUT_LINE. */
    private static String plsqlOutput(Connection con) throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (CallableStatement cs = con.prepareCall("{call DBMS_OUTPUT.GET_LINE(?, ?)}")) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.registerOutParameter(2, Types.INTEGER);
            for (int i = 0; i < 200; i++) {
                cs.execute();
                if (cs.getInt(2) != 0) {
                    break;
                }
                sb.append(cs.getString(1)).append('\n');
            }
        }
        return sb.length() == 0 ? "PL/SQL procedure successfully completed." : sb.toString().trim();
    }

    private static String oraMessage(SQLException e) {
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        int nl = m.indexOf('\n');
        return nl > 0 && m.startsWith("ORA-") ? m.substring(0, nl) : m;
    }

    private Topic find(int number) {
        return topics.stream().filter(t -> t.info().number() == number).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("DBMS topic", number));
    }

    // ------------------------------------------------------------------ the 32 topics
    private static Step q(String sql, String note) { return new Step('Q', sql, note); }
    private static Step d(String sql, String note) { return new Step('D', sql, note); }
    private static Step l(String sql, String note) { return new Step('L', sql, note); }
    private static Step c(String sql, String note) { return new Step('C', sql, note); }
    private static Step p(String sql, String note) { return new Step('P', sql, note); }
    private static Step e(String sql, String note) { return new Step('E', sql, note); }
    private static Step i(String sql) { return new Step('I', sql, ""); }

    private void add(int n, String category, String title, String file, String projectUse, String explanation,
                     String say, String viva, String expected, Step... steps) {
        List<String> sql = new ArrayList<>();
        for (Step s : steps) {
            if (s.kind() != 'I') {
                sql.add(s.sql());
            }
        }
        DemoTopic info = new DemoTopic("t" + n, n, category, title, file, projectUse, explanation, say, viva, expected, sql);
        topics.add(new Topic(info, List.of(steps)));
    }

    private void build() {
        final String NEW_STUDENT = "INSERT INTO student (student_id, student_name, email, dept_id, class_id, joined_year) "
                + "VALUES (902, 'Demo Student', 'demo902@college.edu', 1, 1, 2026)";

        add(1, "SQL basics", "DDL (CREATE / ALTER / TRUNCATE / DROP)", "database/01_tables.sql",
                "All 17 project tables were created with CREATE TABLE.",
                "DDL defines structure. Oracle commits DDL automatically. The demo uses a throw-away table DEMO_TMP.",
                "DDL changes the structure; these 17 tables are the project's DDL.",
                "Why can DDL not be rolled back? Because Oracle issues an implicit COMMIT before and after DDL.",
                "Table created, a column added, columns listed, table truncated and dropped.",
                i("DROP TABLE demo_tmp PURGE"),
                l("CREATE TABLE demo_tmp (id NUMBER PRIMARY KEY, note VARCHAR2(30))", "CREATE"),
                l("ALTER TABLE demo_tmp ADD (created DATE DEFAULT SYSDATE)", "ALTER"),
                q("SELECT column_name, data_type FROM user_tab_columns WHERE table_name = 'DEMO_TMP' ORDER BY column_id", "data dictionary"),
                l("TRUNCATE TABLE demo_tmp", "TRUNCATE"),
                l("DROP TABLE demo_tmp PURGE", "DROP"));

        add(2, "SQL basics", "DML (INSERT / UPDATE / DELETE)", "database/04_sample_data.sql",
                "Every screen that adds, edits or deletes a record runs DML through JdbcTemplate.update().",
                "DML changes rows and can be rolled back until COMMIT.",
                "DML changes data; the app sends it with jdbc.update(sql, args).",
                "Difference between DELETE and TRUNCATE? DELETE is DML, can roll back, can filter; TRUNCATE is DDL.",
                "1 row inserted, updated, shown, deleted; then rolled back.",
                d("INSERT INTO department (dept_id, dept_code, dept_name) VALUES (99, 'DEMO', 'Demo Department')", "INSERT"),
                d("UPDATE department SET dept_name = 'Demo Department (edited)' WHERE dept_id = 99", "UPDATE"),
                q("SELECT * FROM department WHERE dept_id = 99", "see the change"),
                d("DELETE FROM department WHERE dept_id = 99", "DELETE"),
                c("ROLLBACK", "nothing was kept"));

        add(3, "Constraints", "Primary key", "database/01_tables.sql",
                "Every table has a PK, e.g. student_id. The PK identifies one student.",
                "A PK is UNIQUE + NOT NULL and creates an index automatically.",
                "A second department with id 1 is rejected by the PK.",
                "Can a table have two primary keys? No - one PK, but it can be composite.",
                "ORA-00001 unique constraint (PK_DEPARTMENT) violated.",
                q("SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type = 'P' ORDER BY table_name", "all primary keys"),
                e("INSERT INTO department (dept_id, dept_code, dept_name) VALUES (1, 'XXX', 'Duplicate key')", "same dept_id"));

        add(4, "Constraints", "Foreign key", "database/02_constraints.sql",
                "student.dept_id references department. A student cannot belong to a missing department.",
                "A FK allows only values that exist in the parent table (or NULL).",
                "Student in department 999 is rejected.",
                "What is the difference between PK and FK? PK identifies a row; FK links to another table's PK.",
                "ORA-02291 integrity constraint (FK_STUDENT_DEPT) violated - parent key not found.",
                e("INSERT INTO student (student_id, student_name, email, dept_id, class_id) VALUES (900, 'No Dept', 'nodept@college.edu', 999, 1)", "dept 999 does not exist"));

        add(5, "Constraints", "Unique", "database/02_constraints.sql",
                "student.email, department.dept_code, payroll(emp_id, pay_month) are UNIQUE.",
                "UNIQUE allows NULL but no duplicates.",
                "The same department code twice is blocked by UQ_DEPT_CODE.",
                "UNIQUE vs PK? UNIQUE can be NULL and a table may have many.",
                "ORA-00001 unique constraint (UQ_DEPT_CODE) violated.",
                e("INSERT INTO department (dept_id, dept_code, dept_name) VALUES (50, 'CSE', 'Another CSE')", "code CSE already used"));

        add(6, "Constraints", "NOT NULL", "database/01_tables.sql",
                "Names, e-mails, marks and salaries are mandatory.",
                "NOT NULL forbids empty values in a column.",
                "A department without a name is rejected.",
                "Is NULL equal to zero? No, NULL means unknown/absent.",
                "ORA-01400 cannot insert NULL into DEPT_NAME.",
                e("INSERT INTO department (dept_id, dept_code) VALUES (51, 'ZZ')", "dept_name missing"));

        add(7, "Constraints", "CHECK", "database/02_constraints.sql",
                "E-mail format, credits 1-6, attendance status, marks >= 0, net salary <= gross.",
                "CHECK validates a rule on every insert/update.",
                "An e-mail without @ is rejected by CHK_STUDENT_EMAIL.",
                "Can CHECK use another table? No - use a trigger instead.",
                "ORA-02290 check constraint (CHK_STUDENT_EMAIL) violated.",
                q("SELECT constraint_name, search_condition_vc FROM user_constraints WHERE constraint_type = 'C' AND constraint_name LIKE 'CHK%' ORDER BY 1", "all CHECK rules"),
                e("INSERT INTO student (student_id, student_name, email, dept_id, class_id) VALUES (901, 'Bad Mail', 'not-an-email', 1, 1)", "no @ in e-mail"));

        add(8, "Constraints", "Referential integrity", "database/02_constraints.sql",
                "A department with students cannot be deleted; the API turns this into HTTP 409.",
                "Referential integrity = parent/child rows always agree.",
                "Deleting department 1 fails because students refer to it.",
                "What does ON DELETE CASCADE do? Deletes the child rows too (used only for attendance_record).",
                "ORA-02292 integrity constraint violated - child record found.",
                e("DELETE FROM department WHERE dept_id = 1", "students refer to it"));

        add(9, "Queries", "WHERE", "database/05_queries.sql",
                "Student search, low-attendance filter, month filter for payroll.",
                "WHERE filters rows before grouping. Operators: =, <>, BETWEEN, IN, LIKE, AND/OR.",
                "Filtering with LIKE, BETWEEN and IN.",
                "WHERE vs HAVING? WHERE filters rows, HAVING filters groups.",
                "Only students that satisfy the conditions.",
                q("SELECT student_id, student_name FROM student WHERE dept_id = 1 AND student_name LIKE 'R%'", "LIKE"),
                q("SELECT student_id, student_name, joined_year FROM student WHERE joined_year BETWEEN 2024 AND 2025 ORDER BY student_id", "BETWEEN"),
                q("SELECT subject_code, credits FROM subject WHERE credits IN (3, 4) ORDER BY subject_code", "IN"));

        add(10, "Queries", "Aggregate functions", "database/05_queries.sql",
                "Dashboard counts, average marks, pass percentage.",
                "COUNT, SUM, AVG, MIN, MAX return one value for many rows.",
                "Statistics of final marks.",
                "Do aggregates count NULL? COUNT(col) skips NULL, COUNT(*) does not.",
                "One row of statistics.",
                q("SELECT COUNT(*) AS results, ROUND(AVG(final_mark),2) AS average, MIN(final_mark) AS lowest, MAX(final_mark) AS highest, SUM(credits) AS credits FROM result", "aggregates"));

        add(11, "Queries", "GROUP BY", "database/05_queries.sql",
                "Students per department, average mark per subject.",
                "GROUP BY makes one output row per group.",
                "Average final mark of each subject.",
                "Which columns may appear in SELECT? Grouped columns and aggregates.",
                "One row per subject.",
                q("SELECT s.subject_code, COUNT(*) AS students, ROUND(AVG(r.final_mark),2) AS average FROM result r JOIN subject s ON s.subject_id = r.subject_id GROUP BY s.subject_code ORDER BY s.subject_code", "per subject"));

        add(12, "Queries", "HAVING", "database/05_queries.sql",
                "Low attendance report: groups below 75 %.",
                "HAVING filters after GROUP BY.",
                "Students whose attendance is below 75 %.",
                "Can HAVING be used without GROUP BY? Yes, but it is rare.",
                "Only students below 75 %.",
                q("SELECT student_id, ROUND(100 * SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*), 2) AS pct FROM attendance_record GROUP BY student_id HAVING 100 * SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*) < 75 ORDER BY pct", "attendance < 75"));

        add(13, "Queries", "Set operations", "database/05_queries.sql",
                "Combine people lists, find departments with / without data.",
                "UNION, INTERSECT, MINUS combine two queries of the same shape.",
                "UNION lists all e-mails, INTERSECT and MINUS compare departments.",
                "UNION vs UNION ALL? UNION removes duplicates.",
                "Three result sets.",
                q("SELECT email FROM student UNION SELECT email FROM faculty UNION SELECT email FROM employee ORDER BY 1", "UNION"),
                q("SELECT dept_id FROM student INTERSECT SELECT dept_id FROM subject ORDER BY 1", "INTERSECT"),
                q("SELECT dept_id FROM department MINUS SELECT dept_id FROM student ORDER BY 1", "MINUS"));

        add(14, "Queries", "Joins", "database/05_queries.sql",
                "Student list with department and class names; result with subject names.",
                "A join combines rows of several tables using a key.",
                "INNER JOIN, LEFT JOIN and a 3-table join.",
                "INNER vs LEFT? LEFT keeps rows without a match.",
                "Rows combined from several tables.",
                q("SELECT s.student_id, s.student_name, d.dept_code FROM student s JOIN department d ON d.dept_id = s.dept_id ORDER BY s.student_id FETCH FIRST 8 ROWS ONLY", "INNER JOIN"),
                q("SELECT d.dept_code, COUNT(s.student_id) AS students FROM department d LEFT JOIN student s ON s.dept_id = d.dept_id GROUP BY d.dept_code ORDER BY 1", "LEFT JOIN"),
                q("SELECT s.student_name, sub.subject_code, r.final_mark, r.grade_code FROM result r JOIN student s ON s.student_id = r.student_id JOIN subject sub ON sub.subject_id = r.subject_id ORDER BY r.final_mark DESC FETCH FIRST 5 ROWS ONLY", "3 tables"));

        add(15, "Queries", "Subqueries", "database/05_queries.sql",
                "Students above average, students without marks, top mark.",
                "A subquery is a query inside another query.",
                "Single-row, multi-row and correlated subqueries.",
                "Correlated subquery? It uses a column from the outer query and runs per row.",
                "Rows selected by the inner result.",
                q("SELECT student_id, subject_id, final_mark FROM result WHERE final_mark > (SELECT AVG(final_mark) FROM result) ORDER BY final_mark DESC FETCH FIRST 5 ROWS ONLY", "single-row"),
                q("SELECT student_name FROM student WHERE student_id IN (SELECT student_id FROM result WHERE pass_fail = 'FAIL')", "multi-row IN"),
                q("SELECT s.student_name FROM student s WHERE EXISTS (SELECT 1 FROM attendance_record a WHERE a.student_id = s.student_id AND a.status = 'ABSENT') FETCH FIRST 5 ROWS ONLY", "correlated"));

        add(16, "PL/SQL", "Functions", "database/06_functions.sql",
                "Attendance %, internal mark, semester mark, grade, SGPA and CGPA are Oracle functions.",
                "A function returns one value and can be used inside SELECT.",
                "Rahul in CS302: internal 33.4 + semester 49.2 = 82.6 (rounds to 83 = grade A+).",
                "Function vs procedure? A function must RETURN a value and can be used in SQL.",
                "internal 33.4, semester 49.2, grade A+ (83 is in 81-90).",
                q("SELECT calculate_internal_mark(101, " + SUBJECT + ") AS internal_40, calculate_semester_mark(101, " + SUBJECT + ") AS semester_60 FROM dual", "mark parts"),
                q("SELECT calculate_grade(82.6) AS grade, calculate_attendance(101) AS attendance_pct, calculate_cgpa(101) AS cgpa FROM dual", "grade, attendance, CGPA"));

        add(17, "PL/SQL", "Stored procedures", "database/07_procedures.sql",
                "calculate_student_result, mark_attendance, generate_payroll are procedures called by Java.",
                "A procedure performs an action and can return values through OUT parameters.",
                "Calculate Rahul's result and a month of payroll with procedures.",
                "What is an OUT parameter? A parameter the procedure fills for the caller.",
                "Result row for Rahul; payroll rows created (rolled back afterwards).",
                p("BEGIN calculate_student_result(101, " + SUBJECT + "); END;", "MERGE inside"),
                q("SELECT internal_mark, semester_mark, final_mark, grade_code, pass_fail FROM result WHERE student_id = 101 AND subject_id = " + SUBJECT, "stored result"),
                p("DECLARE v NUMBER; BEGIN generate_payroll_all('2026-12', v); DBMS_OUTPUT.PUT_LINE('payroll rows created: ' || v); END;", "OUT parameter"));

        add(18, "Transactions", "Transactions", "database/08_transactions.sql",
                "Marks save and payroll-all run as one unit (@Transactional in Spring).",
                "A transaction is all-or-nothing (ACID).",
                "Insert, look, ROLLBACK, look again.",
                "What does ACID stand for? Atomicity, Consistency, Isolation, Durability.",
                "Row visible before rollback, gone after.",
                d("INSERT INTO department (dept_id, dept_code, dept_name) VALUES (98, 'TXN', 'Transaction Demo')", "start"),
                q("SELECT COUNT(*) AS rows_visible FROM department WHERE dept_id = 98", "inside the transaction"),
                c("ROLLBACK", "undo"),
                q("SELECT COUNT(*) AS rows_visible FROM department WHERE dept_id = 98", "after rollback"));

        add(19, "Transactions", "COMMIT", "database/08_transactions.sql",
                "Spring commits when a @Transactional method ends normally.",
                "COMMIT makes changes permanent. (This demo commits a no-op update.)",
                "COMMIT = save permanently.",
                "Does DDL need COMMIT? No, it commits itself.",
                "Update then COMMIT OK.",
                d("UPDATE student SET phone = phone WHERE student_id = 101", "harmless update"),
                c("COMMIT", "permanent"));

        add(20, "Transactions", "ROLLBACK", "database/08_transactions.sql",
                "If a Java exception occurs inside @Transactional, Spring rolls back.",
                "ROLLBACK undoes everything since the last COMMIT.",
                "Change Rahul's CT1, roll back, mark is 27 again.",
                "When does Spring roll back? On RuntimeException by default.",
                "27 -> 5 -> 27.",
                q("SELECT raw_marks FROM marks WHERE student_id = 101 AND subject_id = " + SUBJECT + " AND assessment_id = 2", "before"),
                d("UPDATE marks SET raw_marks = 5 WHERE student_id = 101 AND subject_id = " + SUBJECT + " AND assessment_id = 2", "change"),
                c("ROLLBACK", "undo"),
                q("SELECT raw_marks FROM marks WHERE student_id = 101 AND subject_id = " + SUBJECT + " AND assessment_id = 2", "after"));

        add(21, "Transactions", "SAVEPOINT", "database/08_transactions.sql",
                "Partial undo inside one unit of work.",
                "SAVEPOINT marks a point; ROLLBACK TO returns to it without losing earlier work.",
                "CT1 change is kept, CT2 change undone.",
                "ROLLBACK vs ROLLBACK TO? The second undoes only back to the savepoint.",
                "CT1 = 10, CT2 = 24 (original).",
                d("UPDATE marks SET raw_marks = 10 WHERE student_id = 101 AND subject_id = " + SUBJECT + " AND assessment_id = 2", "CT1 -> 10"),
                c("SAVEPOINT after_ct1", "mark a point"),
                d("UPDATE marks SET raw_marks = 5 WHERE student_id = 101 AND subject_id = " + SUBJECT + " AND assessment_id = 5", "CT2 -> 5"),
                c("ROLLBACK TO after_ct1", "undo only CT2"),
                q("SELECT a.assessment_code, m.raw_marks FROM marks m JOIN assessment a ON a.assessment_id = m.assessment_id WHERE m.student_id = 101 AND m.subject_id = " + SUBJECT + " AND a.assessment_code IN ('CT1','CT2') ORDER BY 1", "CT1 kept, CT2 original"),
                c("ROLLBACK", "clean up"));

        add(22, "Security", "GRANT", "database/14_users_roles.sql",
                "Student role may only read; faculty role may update marks and attendance.",
                "GRANT gives privileges to a user or role.",
                "Give the student role SELECT on the result view.",
                "Role vs user? A role is a named set of privileges granted to users.",
                "Privilege listed in USER_TAB_PRIVS_MADE.",
                l("GRANT SELECT ON student_result_view TO college_student_role", "needs 14_users_roles.sql"),
                q("SELECT grantee, table_name, privilege FROM user_tab_privs_made WHERE table_name = 'STUDENT_RESULT_VIEW'", "who has access"));

        add(23, "Security", "REVOKE", "database/14_users_roles.sql",
                "Removing a privilege when a role changes.",
                "REVOKE removes a privilege.",
                "Take SELECT away again.",
                "Does REVOKE remove access at once? Yes for new statements.",
                "Privilege no longer listed.",
                l("REVOKE SELECT ON student_result_view FROM college_student_role", "undo grant"),
                q("SELECT grantee, table_name, privilege FROM user_tab_privs_made WHERE table_name = 'STUDENT_RESULT_VIEW'", "no rows"),
                l("GRANT SELECT ON student_result_view TO college_student_role", "restore"));

        add(24, "Triggers", "INSERT trigger", "database/09_triggers.sql",
                "trg_student_audit_ins writes a row to AUDIT_LOG when a student is added.",
                "A trigger fires automatically on INSERT.",
                "Add a student and watch the audit row appear.",
                "Row-level vs statement-level? Row-level fires once per row (FOR EACH ROW).",
                "One INSERT row in audit_log.",
                d(NEW_STUDENT, "INSERT fires trigger"),
                q("SELECT table_name, operation, record_key, new_value FROM audit_log WHERE student_id = 902", "audit row"),
                c("ROLLBACK", "clean up"));

        add(25, "Triggers", "UPDATE trigger", "database/09_triggers.sql",
                "trg_marks_validate blocks marks above the maximum; trg_marks_audit_update logs changes.",
                "BEFORE trigger validates, AFTER trigger audits.",
                "CT1 99 is rejected (max 30); CT1 26 is allowed and audited.",
                "BEFORE vs AFTER? BEFORE can stop/modify the row, AFTER reacts.",
                "ORA-20001 then a logged UPDATE.",
                e("UPDATE marks SET raw_marks = 99 WHERE student_id = 101 AND subject_id = " + SUBJECT + " AND assessment_id = 2", "above max 30"),
                d("UPDATE marks SET raw_marks = 26 WHERE student_id = 101 AND subject_id = " + SUBJECT + " AND assessment_id = 2", "allowed"),
                q("SELECT table_name, operation, old_value, new_value FROM audit_log WHERE table_name = 'MARKS' ORDER BY audit_id DESC FETCH FIRST 3 ROWS ONLY", "audit row"),
                c("ROLLBACK", "clean up"));

        add(26, "Triggers", "DELETE trigger", "database/09_triggers.sql",
                "trg_student_audit_del keeps the old values of a deleted student.",
                "A DELETE trigger uses :OLD values.",
                "Add and delete a student; the audit shows both.",
                "What is :OLD? The row before the change.",
                "INSERT and DELETE rows in audit_log.",
                d(NEW_STUDENT, "add"),
                d("DELETE FROM student WHERE student_id = 902", "delete"),
                q("SELECT operation, record_key, old_value FROM audit_log WHERE student_id = 902 ORDER BY audit_id", "audit"),
                c("ROLLBACK", "clean up"));

        add(27, "Cursors", "Implicit cursor", "database/10_cursors.sql",
                "Every UPDATE/INSERT/DELETE in PL/SQL uses an implicit cursor (SQL%ROWCOUNT).",
                "Oracle opens, fetches and closes implicit cursors itself.",
                "SQL%FOUND and SQL%ROWCOUNT after an UPDATE.",
                "Name two implicit cursor attributes. SQL%FOUND, SQL%NOTFOUND, SQL%ROWCOUNT.",
                "Text with rowcount.",
                p("DECLARE v VARCHAR2(4000); BEGIN demo_implicit_cursor(v); DBMS_OUTPUT.PUT_LINE(v); END;", "procedure demo_implicit_cursor"));

        add(28, "Cursors", "Explicit cursor", "database/10_cursors.sql",
                "department_report_text builds the department report row by row.",
                "Explicit cursor: DECLARE, OPEN, FETCH, EXIT WHEN %NOTFOUND, CLOSE.",
                "Department 1 report produced by a cursor loop.",
                "Why use an explicit cursor? To process a multi-row query row by row.",
                "A text report of the students of the CSE department.",
                p("DECLARE v VARCHAR2(4000); BEGIN department_report_text(1, v); DBMS_OUTPUT.PUT_LINE(v); END;", "procedure department_report_text"));

        add(29, "Exceptions", "Predefined exceptions", "database/11_exceptions.sql",
                "Services show Oracle messages as readable errors.",
                "Predefined: NO_DATA_FOUND, TOO_MANY_ROWS, DUP_VAL_ON_INDEX ...",
                "Three predefined exceptions caught in PL/SQL.",
                "What is WHEN OTHERS? A catch-all handler.",
                "Three messages.",
                p("DECLARE m VARCHAR2(200); BEGIN demo_exception('NO_DATA_FOUND', m); DBMS_OUTPUT.PUT_LINE(m); demo_exception('TOO_MANY_ROWS', m); DBMS_OUTPUT.PUT_LINE(m); demo_exception('DUP_VAL_ON_INDEX', m); DBMS_OUTPUT.PUT_LINE(m); END;", "predefined"));

        add(30, "Exceptions", "Non-predefined exceptions", "database/11_exceptions.sql",
                "ORA-02292 and ORA-02291 are given names with PRAGMA EXCEPTION_INIT; RAISE_APPLICATION_ERROR gives user-defined ones.",
                "Non-predefined errors have a number but no name; PRAGMA EXCEPTION_INIT binds a name.",
                "Child record found, parent not found and a user-defined error.",
                "What is PRAGMA EXCEPTION_INIT? A compiler directive linking a name to an error number.",
                "Three messages.",
                p("DECLARE m VARCHAR2(200); BEGIN demo_exception('CHILD_RECORD_FOUND', m); DBMS_OUTPUT.PUT_LINE(m); demo_exception('PARENT_NOT_FOUND', m); DBMS_OUTPUT.PUT_LINE(m); demo_exception('USER_DEFINED', m); DBMS_OUTPUT.PUT_LINE(m); END;", "non-predefined"));

        add(31, "Objects", "Views", "database/12_views.sql",
                "Result, attendance and CGPA screens read from views.",
                "A view is a stored SELECT that acts like a table.",
                "Query the CGPA view.",
                "Can a view be updated? Simple views yes; joins usually not.",
                "One row per student with CGPA.",
                q("SELECT * FROM student_cgpa_view FETCH FIRST 8 ROWS ONLY", "student_cgpa_view"),
                q("SELECT view_name FROM user_views ORDER BY 1", "all views"));

        add(32, "Objects", "Indexes", "database/13_indexes.sql",
                "Student name search and audit lookups use indexes.",
                "An index lets Oracle find rows without reading the whole table.",
                "EXPLAIN PLAN shows INDEX RANGE SCAN for a name search.",
                "When is an index not used? Tiny tables or when the column is wrapped in a function.",
                "Plan lines mentioning IDX_STUDENT_NAME (or TABLE ACCESS on tiny tables).",
                q("SELECT index_name, table_name FROM user_indexes WHERE table_name IN ('STUDENT','AUDIT_LOG','ATTENDANCE_RECORD') ORDER BY 1", "indexes"),
                d("EXPLAIN PLAN FOR SELECT * FROM student WHERE student_name = 'Rahul'", "plan"),
                q("SELECT plan_table_output FROM TABLE(DBMS_XPLAN.DISPLAY)", "plan output"));
    }
}
