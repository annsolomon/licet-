#!/usr/bin/env bash
# One command to start everything:   ./run.sh
#   ./run.sh --skip-tests   start without running the unit tests first
#   ./run.sh test           run ./test.sh
#   ./run.sh syllabus       run the 15 Java syllabus programs in the terminal (no database needed)
set -uo pipefail
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
cd "$ROOT"

if [ "${1:-}" = "test" ]; then exec ./test.sh; fi

if [ "${1:-}" = "syllabus" ]; then
  command -v javac >/dev/null || die "JDK 21 is required"
  out="$RUN_DIR/syllabus-classes"; rm -rf "$out"; mkdir -p "$out"
  files="$(find backend/src/main/java/com/college \( -path '*/basics/*' -o -path '*/util/*' -o -path '*/marks/*' -o -path '*/payroll/*' -o -path '*/employee/*' -o -path '*/result/*' -o -path '*/attendance/*' -o -path '*/model/*' -o -name '*Exception.java' \) -name '*.java' ! -name GlobalExceptionHandler.java)"
  # shellcheck disable=SC2086
  javac -d "$out" $files && java -cp "$out" com.college.basics.SyllabusRunner
  exit $?
fi

SKIP_TESTS=0; [ "${1:-}" = "--skip-tests" ] && SKIP_TESTS=1

say "Checking prerequisites"
for c in java mvn node npm docker curl; do
  command -v "$c" >/dev/null && ok "$c found" || die "$c is missing. Codespaces: reopen in the dev container. Elsewhere: install JDK 21, Maven 3.9+, Node 20+, Docker."
done
java -version 2>&1 | grep -q '"2[1-9]\|"[3-9][0-9]' || warn "Java 21 is expected (found: $(java -version 2>&1 | head -1))"

say "Oracle database"
if docker ps --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER"; then
  ok "container already running"
elif docker ps -a --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER"; then
  docker start "$ORACLE_CONTAINER" >/dev/null && ok "container started"
else
  echo "  pulling and starting $ORACLE_IMAGE (first time: a few minutes, ~2 GB download)"
  docker run -d --name "$ORACLE_CONTAINER" -p "$ORACLE_PORT:1521" \
    -e ORACLE_PASSWORD="$ORACLE_SYS_PASSWORD" "$ORACLE_IMAGE" >/dev/null || die "docker run failed"
fi
printf '  waiting for Oracle'
for i in $(seq 1 90); do oracle_up && break; printf '.'; sleep 5; [ "$i" = 90 ] && die "Oracle is not ready (docker logs $ORACLE_CONTAINER)"; done
echo; ok "Oracle is accepting connections"

if echo "SELECT 'LOADED' FROM all_users WHERE username='COLLEGE';" | sys_sql | grep -q LOADED \
   && echo "SELECT 'DATA' FROM app_user WHERE ROWNUM = 1;" | app_sql 2>/dev/null | grep -q DATA; then
  ok "COLLEGE schema already loaded"
else
  ./setup-db.sh || die "Database setup failed"
fi

if [ "$SKIP_TESTS" = 0 ]; then
  say "Unit tests (JUnit)"
  (cd backend && mvn -q -B test) && ok "unit tests passed" || die "unit tests failed (run ./run.sh --skip-tests to start anyway)"
fi

./stop.sh --quiet
say "Starting Spring Boot backend (port 8080)"
(cd backend && nohup mvn -q -B spring-boot:run > "$LOG_DIR/backend.log" 2>&1 & echo $! > "$RUN_DIR/backend.pid")
printf '  waiting for the API'
for i in $(seq 1 90); do
  curl -fs http://localhost:8080/api/health 2>/dev/null | grep -q '"database":"UP"' && break
  printf '.'; sleep 3
  [ "$i" = 90 ] && { echo; tail -30 "$LOG_DIR/backend.log"; die "Backend did not become healthy (see logs/backend.log)"; }
done
echo; ok "backend is up and Oracle answers"

say "Starting React frontend (port 5173)"
[ -d frontend/node_modules ] || (cd frontend && npm install --no-audit --no-fund >> "$LOG_DIR/frontend.log" 2>&1)
(cd frontend && nohup npm run dev > "$LOG_DIR/frontend.log" 2>&1 & echo $! > "$RUN_DIR/frontend.pid")
for i in $(seq 1 30); do curl -fs http://localhost:5173 >/dev/null 2>&1 && break; sleep 1; done
ok "frontend is up"

cat <<MSG

  =====================================================================
   College Academic Management System is running
     App       : http://localhost:5173      (Codespaces: open the forwarded port 5173)
     API       : http://localhost:8080/api/health
     Login     : admin / admin123      or      faculty / faculty123
     Logs      : logs/backend.log   logs/frontend.log
     Stop      : ./stop.sh            Tests: ./test.sh        Reset data: ./reset-demo.sh
  =====================================================================
MSG
