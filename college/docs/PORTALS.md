# HOD, Parent and Advisor portals

Three separate web apps, each with its own Spring Boot service, plus a notification service. One jar (`portals/`), four profiles.

| Portal | Web app | Service | Sign in with |
|---|---|---|---|
| HOD | 5174 | 8082 | `hod.cse` / `hod123` (also `hod.ece`, `hod.eee`, `hod.mech`) |
| Parent | 5175 | 8083 | `p101` / `parent123` (`p<studentId>`; `p104` has low attendance) |
| Class advisor | 5176 | 8084 | `advisor.cse` / `advisor123` (also `.ece`, `.eee`, `.mech`) |
| Student | 5177 | 8086 | `s<studentId>` / `student123` (e.g. `s104`) |
| Notifier | – | 8085 | no UI: `GET /api/stats`, `/api/recent`, `POST /api/dispatch`, `/api/retry` (ADMIN login) |

The original admin/faculty app (5173 / 8080) is unchanged; HOD, advisor and parent accounts cannot sign in there.

## Internals
Assignment 1/2 (/40), class test 1/2 (/30) and CAT 1/2 (/60) for every student and subject. Internal mark /40 = (part 1 + part 2) / 5 with part = assignment + ⅔ (class test + CAT), the formula of `calculate_internal_mark`; pass mark 20. Visible to the student, the parent, the advisor (class grid) and the HOD (department: Students and Internals pages).

## Data
* 60 students in each of the 4 departments (240 total), one parent per student.
* 8 periods a day (09:00 – 16:50), each class has a timetable of 6 subjects.
* 10 weekdays of attendance, one `attendance_session` per class per period and one `attendance_record` per student: about 19,200 rows.
* "Present" means status `PRESENT`; OD, medical and other count as not present, the same rule as the main application.

## How alerts work
1. Any program marks a student `ABSENT` in `attendance_record` (the advisor portal, the main app, or plain SQL).
2. The Oracle trigger `trg_attendance_notify` calls `notify_absence`, which writes two rows to `notification`: one SMS per absent student to the parent. For the class advisor the trigger builds one e-mail per marked period listing all absentees of that period (re-sent with the corrected list if the marking changes). `UNIQUE(record_id, recipient_type)` and a unique index per period make it idempotent.
3. The notification service polls `PENDING` rows every 3 s and sends them. By default it writes to `output/notifications.log`. To use a real gateway, start with `NOTIFY_WEBHOOK_URL=https://...`; each alert is POSTed as JSON. Failures are marked `FAILED` and can be retried from the notifier API.
4. Parent and advisor see the alert in their portal's Alerts tab with an unread badge.

Demo history was loaded before the trigger existed, so only the latest day's absences were queued.

## Demo
`docs/DEMO.md` (browser walkthrough) and `./portals-demo.sh` (terminal).

## Run
```
./portals-setup-db.sh     # once: loads database/16..18
./portals-run.sh          # start (also: stop, status)
```
Re-running `portals-setup-db.sh` rebuilds the whole database from scratch.
