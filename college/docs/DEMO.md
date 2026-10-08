# Manual demo: attendance marked, alerts received

Start everything: `./portals-run.sh`. Open three browser tabs (Codespaces: the forwarded ports).

| Who | URL | Login |
|---|---|---|
| Class advisor | :5176 | `advisor.cse` / `advisor123` |
| Parent of Divya (student 104) | :5175 | `p104` / `parent123` |
| Parent of Nisha (student 110) | :5175, private window | `p110` / `parent123` |
| HOD | :5174 | `hod.cse` / `hod123` |
| Student Divya (104) | :5177 | `s104` / `student123` |

1. **Parent tab (p104), before.** Today shows the latest day. Alerts has the earlier alerts.
2. **Advisor tab.** Click **Mark a period**. The date is already the next school day with no attendance. Choose period 1, tick Rahul (101), Divya (104), Ravi (109) and Nisha (110), then **Save & send alerts**.
3. Repeat for period 2 (tick 101, 104, 109) and period 3 (tick 104 only).
4. Wait about 5 seconds. The notification service picks up what the database trigger queued.
5. **Advisor → Alerts.** One e-mail alert per period, each listing all absentees of that period:
   "Absentees in period 1 - CS304 ... (4 of N): Rahul (#101), Divya (#104), ..."
6. **Parent tab (p104) → Alerts.** One SMS per absent period (periods 1, 2, 3). The bell shows the unread count. **Today** shows the red "absent in 3 periods" banner and the 8-period strip. **Subjects** and **History** have changed too.
7. **Parent p110.** Only period 1 (Nisha was back for periods 2 and 3).
8. **Correction.** Advisor → Periods → click the date and open that day's grid. Flip Divya's period 3 box to Present. The advisor gets an updated digest for period 3 ("0 absent, corrected") and Divya's parent keeps the earlier SMS.
9. **HOD tab.** Overview, pick that date: attendance drops, the period bars show P1 to P3, Absentees lists the four students, and the alert counters grow.
10. **Internals.** Parent p104 → Internals, student s104 → Internals, advisor → Internals (class grid, click a student) and HOD → Students / Internals (click any student): the same assignment, class test and CAT marks and the internal mark out of 40 (pass mark 20) in every portal.
11. **Student portal (:5177).** Divya sees her own 8-period strip, attendance by subject and internals. No alerts: those go to the parent and advisor.
12. **Delivery log.** `tail -f output/notifications.log` shows each SMS / e-mail as the notifier sends it.

The same flow without the browser: `./portals-demo.sh`
