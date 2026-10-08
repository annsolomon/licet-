#!/usr/bin/env bash
# HOD, Parent, Advisor portals + Notification service, each on its own port.
#   ./portals-run.sh            start everything   (also: start)
#   ./portals-run.sh stop       stop the 4 services and 3 web apps
#   ./portals-run.sh status     show what is up
set -uo pipefail
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
cd "$ROOT"

# name  profile  api-port  ui-port
SERVICES=("hod hod 8082 5174" "parent parent 8083 5175" "advisor advisor 8084 5176" "student student 8086 5177" "notify notify 8085 -")

stop_all() {
  for s in "${SERVICES[@]}"; do
    read -r name _ api ui <<<"$s"
    for pf in "$RUN_DIR/portal-$name.pid" "$RUN_DIR/ui-$name.pid"; do
      [ -f "$pf" ] && { pid="$(cat "$pf")"; pkill -P "$pid" 2>/dev/null; kill "$pid" 2>/dev/null; rm -f "$pf"; }
    done
    if command -v lsof >/dev/null; then
      lsof -ti tcp:"$api" 2>/dev/null | xargs -r kill 2>/dev/null
      [ "$ui" != "-" ] && lsof -ti tcp:"$ui" 2>/dev/null | xargs -r kill 2>/dev/null
    fi
  done
  return 0
}

status() {
  printf '\n  %-22s %-8s %s\n' SERVICE PORT STATE
  for s in "${SERVICES[@]}"; do
    read -r name _ api ui <<<"$s"
    h="$(curl -fs --max-time 2 "http://localhost:$api/api/health" 2>/dev/null)"
    st="DOWN"; echo "$h" | grep -q '"status":"UP"' && st="UP"; echo "$h" | grep -q '"database":"DOWN"' && st="UP (database DOWN)"
    printf '  %-22s %-8s %s\n' "$name service" "$api" "$st"
    if [ "$ui" != "-" ]; then
      u="DOWN"; curl -fs --max-time 2 "http://localhost:$ui" >/dev/null 2>&1 && u="UP"
      printf '  %-22s %-8s %s\n' "$name web app" "$ui" "$u"
    fi
  done
  echo
}

case "${1:-start}" in
  stop) stop_all; ok "portals stopped"; exit 0 ;;
  status) status; exit 0 ;;
  start) ;;
  *) die "usage: ./portals-run.sh [start|stop|status]" ;;
esac

for g in portals/target portals-ui/node_modules portals-ui/dist; do grep -qxF "$g" .gitignore 2>/dev/null || echo "$g" >> .gitignore; done
say "Checking prerequisites"
for c in java mvn node npm docker curl; do command -v "$c" >/dev/null && ok "$c found" || die "$c is missing"; done

say "Oracle database"
docker ps --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER" || { docker start "$ORACLE_CONTAINER" >/dev/null 2>&1 || die "Oracle container not found. Run ./run.sh once first."; }
for i in $(seq 1 60); do oracle_up && break; printf '.'; sleep 3; [ "$i" = 60 ] && die "Oracle is not ready"; done
chk="$(echo "SELECT 'PORTAL_OK' FROM user_tables WHERE table_name='NOTIFICATION';" | app_sql 2>&1)"
case "$chk" in *PORTAL_OK*) ;; *) die "Portal data is not loaded (or the database user cannot connect). Run ./portals-setup-db.sh first. Output: $chk" ;; esac
ok "Oracle is ready with the portal data"

stop_all
say "Building the portal services (one jar, five profiles)"
mvn -q -B -f portals/pom.xml package -DskipTests > "$LOG_DIR/portals-build.log" 2>&1 || { tail -30 "$LOG_DIR/portals-build.log"; die "Maven build failed (logs/portals-build.log)"; }
export NOTIFY_LOG="$ROOT/output/notifications.log"
JAR="$ROOT/portals/target/portals.jar"; [ -f "$JAR" ] || die "portals.jar was not produced"
ok "built portals.jar"

say "Starting the five services"
for s in "${SERVICES[@]}"; do
  read -r name profile api _ <<<"$s"
  (nohup java -jar "$JAR" --spring.profiles.active="$profile" > "$LOG_DIR/portal-$name.log" 2>&1 & echo $! > "$RUN_DIR/portal-$name.pid")
done
for s in "${SERVICES[@]}"; do
  read -r name _ api _ <<<"$s"
  printf '  waiting for %s (%s)' "$name" "$api"
  for i in $(seq 1 60); do h="$(curl -fs "http://localhost:$api/api/health" 2>/dev/null)"; case "$h" in *'"status":"UP"'*) break ;; esac; printf '.'; sleep 2
    [ "$i" = 60 ] && { echo; tail -25 "$LOG_DIR/portal-$name.log"; die "$name did not start (logs/portal-$name.log)"; }; done
  echo " up"
done

say "Starting the four web apps"
[ -d portals-ui/node_modules ] || (cd portals-ui && npm install --no-audit --no-fund >> "$LOG_DIR/portals-ui.log" 2>&1) || die "npm install failed (logs/portals-ui.log)"
for s in "${SERVICES[@]}"; do
  read -r name _ _ ui <<<"$s"
  [ "$ui" = "-" ] && continue
  (cd portals-ui && nohup npm run "dev:$name" > "$LOG_DIR/ui-$name.log" 2>&1 & echo $! > "$RUN_DIR/ui-$name.pid")
done
for p in 5174 5175 5176 5177; do for i in $(seq 1 30); do curl -fs "http://localhost:$p" >/dev/null 2>&1 && break; sleep 1; done; done

cat <<MSG

  =====================================================================
   Portals are running   (open the forwarded port in Codespaces)
     HOD       http://localhost:5174    hod.cse / hod123      (also hod.ece, hod.eee, hod.mech)
     Parent    http://localhost:5175    p101 / parent123      (p104 = low attendance)
     Advisor   http://localhost:5176    advisor.cse / advisor123  (also .ece .eee .mech)
     Student   http://localhost:5177    s104 / student123     (s<student id>)
     Services  8082 hod   8083 parent   8084 advisor   8086 student   8085 notify
     Alerts    output/notifications.log   (set NOTIFY_WEBHOOK_URL to push to a real SMS/e-mail gateway)
     Logs      logs/portal-*.log    Status: ./portals-run.sh status    Stop: ./portals-run.sh stop
  =====================================================================
MSG
