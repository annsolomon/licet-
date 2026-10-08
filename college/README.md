# College Academic Management System

React + Spring Boot 3 (Java 21) + JdbcTemplate with raw SQL + Oracle (PL/SQL) + Maven. No JPA.
Built so that **every Java and DBMS syllabus topic is demonstrated inside a real application**.

Modules: Login, Dashboard, Departments, Students, Faculty, Subjects, Classes, Attendance, Internal Assessment, Semester Exam,
Results, Grades, SGPA, CGPA, Employees, Payroll, Payslips, Reports, Audit, plus **Java Lab** and **DBMS Lab**.

## Run it (GitHub Codespaces or any machine with Docker)

1. Open the repository in a Codespace (use a **4-core / 8 GB** machine). The `.devcontainer` installs Java 21, Maven, Node 20 and Docker.
2. In the terminal: `./run.sh`  (first run pulls the Oracle Free image, about 2 GB, and loads the database: a few minutes).
3. Open the forwarded port **5173**. Log in with `admin / admin123` (full access) or `faculty / faculty123`.

| Command | What it does |
|---|---|
| `./run.sh` | start Oracle, load the database if needed, run unit tests, start backend (8080) and frontend (5173) |
| `./run.sh --skip-tests` | same, without the unit tests |
| `./run.sh syllabus` | run the 15 Java syllabus programs in the terminal (no database needed) |
| `./test.sh` | PASS/FAIL list: scripts, unit tests, frontend build, API tests against Oracle |
| `./demo-sql.sh 08` | run one SQL demo script and show Oracle's output |
| `./reset-demo.sh` | restore the original demo data |
| `./stop.sh` (`--db` also stops Oracle) | stop everything |

Requirements outside Codespaces: JDK 21, Maven 3.9+, Node 20+, Docker, curl.

## Layout
```
backend/    Spring Boot (controller, service, repository, model, dto, exception, config, util, basics, employee, payroll, marks, result, attendance)
frontend/   React + Vite (pages, components, services, hooks)
database/   00-15 SQL scripts (tables, constraints, data, queries, functions, procedures, transactions, triggers, cursors, exceptions, views, indexes, roles)
tests/      API tests (node --test)
docs/       EVALUATOR_GUIDE.md (explain, demo, viva, PPT), ER_DIAGRAM.md
API.md  SYLLABUS_MAPPING.md
```

## Key design points
* **Layers**: Controller -> Service -> Repository -> JdbcTemplate -> raw SQL -> Oracle. Constructor injection, records for DTOs.
* **Oracle does the heavy work**: functions (`calculate_internal_mark`, `calculate_grade`, `calculate_sgpa`, `calculate_cgpa`...), procedures with OUT parameters, MERGE, triggers (validation + audit), views, indexes, roles.
* **Java does the same maths too** (`MarkCalculator`, `PayrollCalculator`, `GradePointCalculator`) and the Marks page shows both side by side.
* **Errors**: one JSON shape with a `layer` field (Java validation / Oracle constraint / Trigger / PL/SQL exception / Database connection).
* **Security**: SHA-256 hashed passwords, bearer token, role checks (ADMIN, FACULTY) in an interceptor, SQL only through `?` parameters, report file names whitelisted.
* **Sample data**: 10 students, 6 faculty, 4 departments, 8 subjects, 5 employees; Rahul / CS302 reproduces the specification example (33.4 + 49.2 = 82.6); Divya has low attendance; September 2026 payroll is pre-generated.

## Demo accounts
`admin / admin123`, `faculty / faculty123`. Oracle: `college / college123` on `localhost:1521/FREEPDB1`; `system / oracle123`.

## Honest status of verification
Checked in the build environment: all Java sources compile (the Spring-dependent files were compiled against stand-in Spring classes), 22 unit tests pass (including 82.6 and payroll 104000 / 94400), the 15 Java syllabus programs run, the React app builds and all 16 pages render against a mock API.

**Not run yet** (no Oracle and no Maven Central access were available when this was built): the SQL/PL-SQL scripts against a real Oracle, the Spring Boot application start-up, and `tests/api.test.mjs`. Run `./run.sh`, then `./test.sh`. `setup-db.sh` stops at the first failing script and lists `user_errors`; if a script reports an error, fix that line and rerun `./setup-db.sh`.

See `docs/EVALUATOR_GUIDE.md` for the explanation script, demo steps and viva questions.
