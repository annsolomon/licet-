# REST API

Base URL `http://localhost:8080`. All calls except login and health need `Authorization: Bearer <token>`.
Errors always look like `{timestamp, status, error, message, layer}`; `layer` is one of
`Java validation`, `Spring exception handling`, `Oracle constraint`, `Trigger`, `PL/SQL exception`, `Database connection`.

Roles: **ADMIN** everything. **FACULTY** may read everything except employees / payroll / audit, and may write attendance, marks, results, reports and the labs.

| Method & path | Purpose |
|---|---|
| POST `/api/auth/login` `{username,password}` | token + role |
| POST `/api/auth/logout`, GET `/api/auth/me` | |
| GET `/api/health` (public) | `{api, database, tables}` |
| GET `/api/dashboard` | counts, attendance, pass %, charts |
| GET/POST `/api/departments`, GET/PUT/DELETE `/api/departments/{id}` | `{code,name}` |
| GET `/api/students?search=&mode=java|sql&departmentId=&classId=` | `mode=java` uses GenericSearch, `sql` uses `LIKE` |
| POST `/api/students[?skipChecks=true]`, PUT/DELETE `/api/students/{id}` | `{id?,name,email,phone,departmentId,classId,joinedYear}`; `skipChecks` lets Oracle reject duplicates |
| GET/POST/PUT/DELETE `/api/faculty` | `{code,name,email,designation,departmentId}` |
| GET/POST/PUT/DELETE `/api/subjects` | `{code,name,credits,semester,departmentId}` |
| GET/POST/PUT/DELETE `/api/classes` | `{name,departmentId,semester,section,academicYear}` |
| GET/POST `/api/classes/assignments`, DELETE `/api/classes/assignments/{id}` | `{classId,subjectId,facultyId}` |
| POST `/api/attendance` | `{classId,subjectId,facultyId,date,period,records:[{studentId,status}]}` status: PRESENT, ABSENT, OD, MEDICAL, OTHER |
| POST `/api/attendance/procedure` | `{sessionId,studentId,status}` via PL/SQL `mark_attendance` |
| GET `/api/attendance/student/{id}` `/subject/{id}` `/class/{id}` `/department/{id}` `/all` `/low?threshold=75` | summaries |
| GET `/api/attendance/student/{id}/records`, `/sessions?classId=&subjectId=`, `/sessions/{id}` | detail |
| GET `/api/marks/assessments`, GET `/api/marks?studentId=&subjectId=` | |
| POST `/api/marks[?skipValidation=true]` `{studentId,subjectId,assessmentCode,rawMarks}` | `skipValidation` lets the Oracle trigger reject |
| GET `/api/marks/breakdown?studentId=&subjectId=` | step-by-step Java calculation vs Oracle functions |
| GET `/api/results?semester=&departmentId=`, GET `/api/results/student/{id}` | results, SGPA per semester, CGPA |
| POST `/api/results/calculate` `{studentId,subjectId}`, POST `/api/results/calculate-all` | PL/SQL procedures |
| GET `/api/results/top?limit=5` | highest CGPA |
| GET `/api/grades`, PUT `/api/grades/{code}` | configurable grade scale |
| GET/POST/PUT/DELETE `/api/employees` (ADMIN) | `{name,email,designation,basicSalary}` |
| GET `/api/payroll?month=YYYY-MM`, GET `/api/payroll/{id}`, GET `/api/payroll/{id}/payslip` (ADMIN) | |
| POST `/api/payroll/generate`, `/generate-all` | Java OOP path `{empId?,month}` |
| POST `/api/payroll/generate-plsql`, `/generate-all-plsql` | PL/SQL path |
| DELETE `/api/payroll?month=YYYY-MM` | |
| GET `/api/reports/types`, POST `/api/reports` `{type,departmentId?,month?,threshold?}`, POST `/api/reports/parallel` `{types:[]}` | |
| GET `/api/reports/files`, `/files/{name}`, `/files/{name}/download`, `/files/{name}/count?character=a`, POST `/files/{name}/copy` | file I/O |
| GET `/api/audit?limit=100` (ADMIN) | rows written by triggers |
| GET `/api/lab/all`, `/factorial?n=`, `/fibonacci?count=`, `/sort?numbers=`, `/binary-search?numbers=&key=`, `/shape?type=&a=&b=`, `/string?text=`, `/student-search?keyword=`, `/jdbc-compare?deptId=` | Java syllabus |
| GET `/api/dblab/topics`, GET `/api/dblab/topics/{n}`, POST `/api/dblab/topics/{n}/run` | DBMS syllabus (rolls back) |

HTTP codes: 200/201/204 success, 400 validation / trigger / PL-SQL error, 401 not logged in, 403 wrong role, 404 not found, 409 duplicate or child rows exist, 503 Oracle down.
