#!/usr/bin/env bash
# Full flow demo from the terminal:  advisor marks periods -> database trigger -> notify service -> what the parents and the advisor receive.
#   ./portals-demo.sh                     marks periods 1-3 of the next free school day for CSE (absent: 101, 104, 109, 110)
#   DEMO_DATE=2026-10-12 ./portals-demo.sh   use another date (weekday)
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
cd "$ROOT"

for p in 8083 8084 8085 8086; do curl -fs "http://localhost:$p/api/health" >/dev/null 2>&1 || die "Service on port $p is not running. Start it with ./portals-run.sh"; done

login() { curl -fs -X POST "http://localhost:$1/api/login" -H 'Content-Type: application/json' -d "{\"username\":\"$2\",\"password\":\"$3\"}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'; }
get()   { curl -fs "http://localhost:$1$2" -H "Authorization: Bearer $3"; }
pretty() { python3 -c '
import sys, json
for n in json.load(sys.stdin)[:int(sys.argv[1])]:
    print("   %-5s to %-26s %s" % (n["channel"], n["destination"], n["title"]))
    print("         " + n["message"])
' "$1"; }

AT="$(login 8084 advisor.cse advisor123)"; [ -n "$AT" ] || die "advisor login failed (is the database loaded? ./portals-setup-db.sh)"
LATEST="$(get 8084 /api/class "$AT" | sed -n 's/.*"latestDate":"\([^"]*\)".*/\1/p')"
D="${DEMO_DATE:-}"
if [ -z "$D" ]; then
  D="$(date -d "$LATEST +1 day" +%F)"
  while [ "$(date -d "$D" +%u)" -gt 5 ]; do D="$(date -d "$D +1 day" +%F)"; done
fi
ABSENT="${DEMO_ABSENT:-101,104,109,110}"

say "1. Class advisor (advisor.cse) marks periods 1-3 on $D.  Absent: $ABSENT"
for P in 1 2 3; do
  # period 2: student 110 is back (shows that the digest lists only that period's absentees)
  A="[$ABSENT]"; [ "$P" = 2 ] && A="[101,104,109]"; [ "$P" = 3 ] && A="[104]"
  r="$(curl -s -X POST http://localhost:8084/api/period/mark -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' \
       -d "{\"date\":\"$D\",\"period\":$P,\"absentIds\":$A}")"
  case "$r" in *'"sessionId"'*) ;; *) die "marking period $P failed: $r  (see logs/portal-advisor.log)" ;; esac
  echo "   period $P -> $r"
done

say "2. The database trigger queued the alerts; the notification service (8085) delivers them"
curl -fs -X POST http://localhost:8085/api/dispatch -H "Authorization: Bearer $(login 8085 admin admin123)" >/dev/null 2>&1
sleep 4

say "3. What parent of student 104 (p104) receives - one SMS per absent period"
PT="$(login 8083 p104 parent123)"; get 8083 "/api/notifications" "$PT" | pretty 5

say "4. What parent of student 110 (p110) receives - only period 1"
PT2="$(login 8083 p110 parent123)"; get 8083 "/api/notifications" "$PT2" | pretty 2

say "5. What the class advisor receives - ONE e-mail per period listing all absentees"
get 8084 "/api/notifications" "$AT" | pretty 3

say "6. Delivery log written by the notification service (output/notifications.log)"
tail -n 8 output/notifications.log 2>/dev/null | cut -c1-230

say "7. Internal marks (/40) of student 104: what the student (s104) and the parent (p104) see"
ST="$(login 8086 s104 student123)"
get 8086 /api/internals "$ST" | python3 -c '
import sys, json
d = json.load(sys.stdin)
print("   average %s / 40, %s subject(s) below the pass mark of %s" % (d["average"], d["below"], d["passMark"]))
for s in d["subjects"]:
    print("   %-6s %-28s ASG1 %-4s CT1 %-4s CAT1 %-4s ASG2 %-4s CT2 %-4s CAT2 %-4s => %s / 40" % (s["code"], s["name"][:28], s["asg1"], s["ct1"], s["cat1"], s["asg2"], s["ct2"], s["cat2"], s["internal"]))
'
echo "   (parent p104, advisor.cse and hod.cse read the same marks in their portals)"

cat <<MSG

  Now look at it in the browsers:
    Advisor   http://localhost:5176  (advisor.cse / advisor123)  -> Periods (date $D) and Alerts
    Parent    http://localhost:5175  (p104 / parent123)           -> Today, History, Alerts
    HOD       http://localhost:5174  (hod.cse / hod123)           -> Overview (pick date $D), Students, Internals
    Student   http://localhost:5177  (s104 / student123)          -> Today, Attendance, Internals, History
  Run again with another date:  DEMO_DATE=<yyyy-mm-dd> ./portals-demo.sh
MSG
