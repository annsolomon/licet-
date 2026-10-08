# Syllabus Mapping

Every syllabus topic has (1) a tiny stand-alone sample and (2) a real use inside the college system.
Run all Java samples without a database: `./run.sh syllabus`. Or open **Java Lab** / **DBMS Lab** in the app.

Paths below are relative to `backend/src/main/java/com/college/`.

## Java syllabus (15 topics)

| # | Topic | Tiny sample (`basics/SyllabusDemos.java`, `BasicPrograms.java`) | Real use in the project | Where to see it |
|---|-------|------|------|------|
| 1 | Factorial | `BasicPrograms.factorial / factorialRecursive` | Java Lab | Java Lab -> Factorial |
| 1 | Fibonacci | `BasicPrograms.fibonacci` | Java Lab | Java Lab -> Fibonacci |
| 1 | Binary search | `BasicPrograms.binarySearch` | Java Lab | Java Lab -> Binary search |
| 1 | Selection sort | `BasicPrograms.selectionSort` | Java Lab | Java Lab -> Sorting |
| 1 | Insertion sort | `BasicPrograms.insertionSort` | Java Lab | Java Lab -> Sorting |
| 2 | OOP principles (class, object, inheritance, polymorphism, abstraction, encapsulation) | `SyllabusDemos.oop()` | `model/Person` -> `Student`, `Faculty`; private fields + getters | Students, Faculty pages |
| 3 | Employee payslip (Employee, Programmer, Assistant/Associate Professor, Professor; gross, net) | `SyllabusDemos.payslip()` | `employee/Employee` (abstract) + 4 subclasses, `EmployeeFactory`, `payroll/PayrollCalculator` | Payroll page (Java button) |
| 4 | Abstract class Shape (`int a; int b; printArea()`) | `basics/Shape`, `Rectangle`, `Triangle`, `Circle` | Java Lab; same abstract pattern as `Employee` | Java Lab -> Shapes |
| 5 | Interfaces | `SyllabusDemos.interfaces()` | `employee/Payable` (Employee implements it), `util/Searchable` (StudentService implements it), `service/ReportGenerator` | Payroll, Students search, Reports |
| 6 | Exception handling (try/catch/finally, custom exceptions) | `SyllabusDemos.exceptions()` | `exception/*` (InvalidMarksException, InvalidStudentException, ...), `GlobalExceptionHandler` | Marks page with a mark above the maximum |
| 7 | String handling | `SyllabusDemos.strings()` | `util/StringUtil` (title case, e-mail check, subject code), `PasswordUtil` | Java Lab -> String; validation of every form |
| 8 | File operations: count character occurrences, copy a file | `SyllabusDemos.files()` | `util/FileUtil` used by `ReportService` (write report, count, copy) | Reports page -> Count / Copy |
| 9 | Generic classes | `util/Pair<A,B>` | `Pair` | Java Lab generics output |
| 10 | Multithreading | `SyllabusDemos.threads()` (Thread, Runnable, ExecutorService) | `ReportService.generateParallel` (ExecutorService) | Reports -> Generate in parallel (shows thread names) |
| 11 | Generic search | `util/GenericSearch<T>` | `StudentService.search` (search students in Java) | Students -> search "in Java" |
| 12 | Generic average | `util/GenericAverage<T extends Number>` | Java Lab generics demo | Java Lab |
| 13 | Java built-in packages (java.util, java.io, java.math, java.time, java.sql) | `SyllabusDemos.packages()` | `BigDecimal` for marks and money, `java.time.LocalDate` for attendance, `java.sql` for JDBC | Everywhere |
| 14 | User-defined packages | `SyllabusDemos.packages()` | `com.college.{model,service,repository,controller,util,...}` | Source tree |
| 15 | Collection framework (List, Set, Map, Iterator, Comparator, Collections.sort) | `SyllabusDemos.collections()` | `ReportService` (Map of generators), `AuthService` (ConcurrentHashMap of tokens), RowMapper results in Lists | Java Lab, Reports |

## DBMS / Oracle syllabus (32 topics)

All 32 run live in **DBMS Lab** (`DbmsDemoService`); every run rolls back. SQL files are in `database/`.

| # | Topic | File | Project use |
|---|-------|------|-----|
| 1 | DDL | 01_tables.sql | 17 tables |
| 2 | DML | 04_sample_data.sql | all create / edit / delete screens |
| 3 | Primary key | 01_tables.sql | every table |
| 4 | Foreign key | 02_constraints.sql | student -> department / class |
| 5 | Unique | 02_constraints.sql | student e-mail, department code, payroll (employee, month) |
| 6 | NOT NULL | 01_tables.sql | mandatory fields |
| 7 | CHECK | 02_constraints.sql | e-mail format, credits, status, marks >= 0 |
| 8 | Referential integrity | 02_constraints.sql | cannot delete a department that has students (HTTP 409) |
| 9 | WHERE | 05_queries.sql | student search, filters |
| 10 | Aggregate functions | 05_queries.sql | dashboard numbers |
| 11 | GROUP BY | 05_queries.sql | average per subject |
| 12 | HAVING | 05_queries.sql | low-attendance report |
| 13 | Set operations | 05_queries.sql | UNION / INTERSECT / MINUS |
| 14 | Joins | 05_queries.sql | result with student + subject names |
| 15 | Subqueries | 05_queries.sql | students above average |
| 16 | Functions | 06_functions.sql | mark, grade, SGPA, CGPA, attendance |
| 17 | Stored procedures | 07_procedures.sql | calculate results, mark attendance, payroll |
| 18 | Transactions | 08_transactions.sql | `@Transactional` in Spring |
| 19 | COMMIT | 08_transactions.sql | end of a transaction |
| 20 | ROLLBACK | 08_transactions.sql | undo on error |
| 21 | SAVEPOINT | 08_transactions.sql | partial undo |
| 22 | GRANT | 14_users_roles.sql | faculty / student roles |
| 23 | REVOKE | 14_users_roles.sql | remove a privilege |
| 24 | INSERT trigger | 09_triggers.sql | `trg_student_audit_ins` |
| 25 | UPDATE trigger | 09_triggers.sql | `trg_marks_validate`, `trg_marks_audit_update` |
| 26 | DELETE trigger | 09_triggers.sql | `trg_student_audit_del` |
| 27 | Implicit cursor | 10_cursors.sql | `SQL%ROWCOUNT` |
| 28 | Explicit cursor | 10_cursors.sql | department report |
| 29 | Predefined exceptions | 11_exceptions.sql | NO_DATA_FOUND, TOO_MANY_ROWS, DUP_VAL_ON_INDEX |
| 30 | Non-predefined exceptions | 11_exceptions.sql | `PRAGMA EXCEPTION_INIT` for -2291 / -2292, `RAISE_APPLICATION_ERROR` |
| 31 | Views | 12_views.sql | result, attendance, CGPA views |
| 32 | Indexes | 13_indexes.sql | student name, audit date, attendance |
