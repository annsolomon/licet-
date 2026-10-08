# Evaluator guide: how to explain and demo this project

## 1. One-minute explanation
"This is a College Academic Management System. A **React** screen calls a **Spring Boot** REST API. The API is layered:
Controller -> Service -> Repository. The Repository uses **JdbcTemplate with raw SQL** (no JPA) against **Oracle**, where
the important calculations (marks, grades, SGPA/CGPA, payroll) are **PL/SQL functions and procedures**, rules are enforced
by **constraints and triggers**, and every change is audited by triggers. Every Java and DBMS syllabus topic has a tiny
working sample (Java Lab, DBMS Lab) **and** a real use in the system."

## 2. Visual flows (say these while pointing at the screen)

```
LOGIN       Login form -> POST /api/auth/login -> AuthController -> AuthService -> UserRepository
            -> SELECT FROM app_user -> SHA-256 compare -> token -> every call sends "Authorization: Bearer"

STUDENT     React form -> StudentController -> StudentService (Java validation) -> StudentRepository
            -> JdbcTemplate.update("INSERT ...") -> Oracle (PK / UNIQUE / FK / CHECK) -> trigger writes audit_log

MARKS       raw marks -> MarksService (MarkValidator) -> MERGE into marks -> trigger trg_marks_validate (ORA-20001)
            -> calculate_internal_mark / calculate_semester_mark / calculate_grade -> MERGE into result

PAYROLL     Employee (abstract) <- Programmer | AssistantProfessor | AssociateProfessor | Professor
            basic -> hra() da() pf() -> gross = basic+hra+da, net = gross-pf -> INSERT payroll (UNIQUE emp+month)
            (or the same job in PL/SQL: generate_payroll)

ERROR       Oracle error -> Spring exception -> GlobalExceptionHandler -> JSON {status,message,layer} -> React alert
```

## 3. The mark formula (memorise one example)
Rahul, CS302: ASG1 35, CT1 27, CAT1 48, ASG2 32, CT2 24, CAT2 51, SEM 82.

```
Part 1 = ASG1 + (CT1 + CAT1) x 2/3 = 35 + (27+48) x 2/3 = 35 + 50 = 85
Part 2 = ASG2 + (CT2 + CAT2) x 2/3 = 32 + (24+51) x 2/3 = 32 + 50 = 82
Internal = (85 + 82) / 5 = 33.4   (out of 40)
Semester = 82 x 0.6      = 49.2   (out of 60)
Final    = 33.4 + 49.2   = 82.6   (out of 100) -> rounds to 83 -> grade A+ (grade point 9)
```
Raw marks are stored; converted marks are always calculated (Java `MarkCalculator` and Oracle functions give the same value; the Marks page shows both).
Payroll: Professor basic 80000: HRA 20% = 16000, DA 10% = 8000, gross 104000, PF 12% = 9600, net 94400.

## 4. Ten-minute demo script
1. `./run.sh` -> open the app, log in `admin / admin123`.
2. **Dashboard**: live counts; open "How this page works".
3. **Students**: add a student; try a bad e-mail (Java validation, layer shown). Tick "let Oracle reject duplicates", add the same e-mail again: Oracle UNIQUE constraint answers (layer = Oracle constraint).
4. **Audit log**: the INSERT you just did was written by a trigger. Delete the student; refresh: DELETE row with old values.
5. **Marks**: student 101 / CS302 shows the step-by-step calculation, 82.6, Java = Oracle. Enter CT1 = 31 (max 30): Java rejects. Tick "skip Java validation", save 31 again: the Oracle trigger rejects (layer = Trigger).
6. **Attendance**: low-attendance view shows Divya below 75 %.
7. **Results**: "Calculate all results" runs the PL/SQL procedure; view SGPA/CGPA; Grades page shows the scale is table data.
8. **Payroll**: generate a new month with Java, then try the same month again (UNIQUE blocks it); open a payslip.
9. **Reports**: generate three reports in parallel (thread names shown), count a character, copy a file.
10. **DBMS Lab**: run topics 3 (primary key error), 21 (savepoint), 25 (update trigger), 28 (explicit cursor). **Java Lab**: shapes, generics, collections.

## 5. If something goes wrong
| Symptom | Fix |
|---|---|
| Red banner "Cannot reach the backend" | `./run.sh` again; read `logs/backend.log` |
| API health says database DOWN | `docker ps` (is `college-oracle` running?), `./setup-db.sh` |
| Data looks changed | `./reset-demo.sh` |
| Port busy | `./stop.sh` |

## 6. Viva questions and short answers
1. **Why JdbcTemplate and not JPA?** The syllabus is SQL and PL/SQL; JdbcTemplate keeps every SQL statement visible and removes the JDBC boilerplate (open/close, try/catch).
2. **What does JdbcTemplate do for you?** Gets a pooled connection, prepares the statement, binds parameters, runs it, maps rows with a RowMapper, closes everything, converts `SQLException` to unchecked `DataAccessException`.
3. **What is a RowMapper?** Converts one ResultSet row into one Java object (e.g. `Student`).
4. **Why layers?** Controller = HTTP, Service = rules, Repository = SQL. Each can be changed or tested alone.
5. **What is dependency injection?** Spring creates objects (beans) and passes them to constructors; `StudentService(StudentRepository r)`.
6. **How do you prevent SQL injection?** Always `?` placeholders (PreparedStatement); the DBMS Lab can only choose a topic number, never send SQL.
7. **Where is validation done?** Java (fast, friendly message) *and* Oracle (CHECK, NOT NULL, triggers) as the last line of defence; the error's `layer` field shows who caught it.
8. **Difference between PK and UNIQUE?** PK = unique + not null, one per table; UNIQUE may be NULL, many per table.
9. **What does a trigger do here?** Validates marks (BEFORE UPDATE), audits student and marks changes (AFTER).
10. **Function vs procedure?** A function returns a value and can be used in SELECT; a procedure performs an action and can return through OUT parameters.
11. **Implicit vs explicit cursor?** Implicit is opened by Oracle for each DML (`SQL%ROWCOUNT`); explicit is declared, opened, fetched and closed by you.
12. **Predefined vs non-predefined exception?** Predefined has a name (NO_DATA_FOUND); non-predefined has only a number, named with `PRAGMA EXCEPTION_INIT`.
13. **What does `@Transactional` do?** Runs the method in one transaction: commit on success, rollback on RuntimeException.
14. **COMMIT vs ROLLBACK vs SAVEPOINT?** Save, undo, partial undo.
15. **Why BigDecimal for marks and money?** `double` cannot represent 0.1 exactly; BigDecimal is exact.
16. **Abstract class vs interface (in your code)?** `Employee` is abstract (shared state + partial behaviour); `Payable` is an interface (a contract: basic, hra, da, pf, gross, net).
17. **Where do you use polymorphism?** `Employee e = EmployeeFactory.create(...)`; `e.hra()` runs the subclass rule.
18. **Where do you use generics?** `GenericSearch<T>`, `GenericAverage<T extends Number>`, `Pair<A,B>`, `Searchable<T>`.
19. **Where do you use multithreading?** Parallel report generation with an `ExecutorService`.
20. **Where do you use file handling?** Reports are written, read, copied and character-counted by `FileUtil`.
21. **Why are raw marks stored separately?** So the conversion rules can change without losing the original data, and any result can be re-calculated.
22. **How is the grade chosen?** `calculate_grade` rounds the final mark and reads the range from table GRADE (configurable).
23. **What is a view?** A stored SELECT used like a table (`student_result_view`).
24. **Why an index?** Faster lookup without scanning the whole table; EXPLAIN PLAN shows it.
25. **GRANT and REVOKE?** Give / remove privileges; roles bundle them (`college_faculty_role`).

## 7. Suggested PowerPoint (12 slides)
1. Title and team. 2. Problem and goals. 3. Architecture diagram (React / Spring Boot / Oracle). 4. Database design (ER diagram). 5. Mark formula with the Rahul example. 6. PL/SQL: functions, procedures, triggers. 7. Java OOP: Person, Employee hierarchy, Shape. 8. Generics, collections, threads, file I/O. 9. Error handling and the `layer` idea. 10. Security: token + roles. 11. Live demo screenshots. 12. Syllabus coverage table, testing and future work.
