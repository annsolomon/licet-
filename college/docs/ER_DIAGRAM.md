# Entity relationship diagram

```
 department 1───< faculty            department 1───< subject
     │ 1                                  │ 1
     ├──< class 1───< student >───1 department
     │       │ 1          │ 1
     │       └──< class_subject >── subject / faculty
     │                    │
 student 1───< marks >───1 assessment (ASG1 CT1 CAT1 ASG2 CT2 CAT2 SEM)
     │ 1        └── subject
     ├──< result >── subject , grade (grade_code)
     └──< attendance_record >───1 attendance_session ──> class, subject, faculty

 employee >── salary_rule (designation)      employee 1───< payroll (UNIQUE emp_id + month)
 app_user (login)                             audit_log (written only by triggers)
```
`<` = many side. Full DDL: `database/01_tables.sql`, keys and rules: `database/02_constraints.sql`.
