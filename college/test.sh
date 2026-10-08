#!/usr/bin/env bash
# Runs every check and prints PASS / FAIL / SKIP for each.   ./test.sh
set -uo pipefail
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
cd "$ROOT"
PASS=0; FAIL=0; SKIP=0
check() {   # check <name> <command...>
  local name="$1"; shift
  if "$@" > "$LOG_DIR/test-last.log" 2>&1; then printf '  \033[32mPASS\033[0m  %s\n' "$name"; PASS=$((PASS+1))
  else printf '  \033[31mFAIL\033[0m  %s   (details: logs/test-%s.log)\n' "$name" "$(echo "$name" | tr -c 'A-Za-z0-9' '-')"; cp "$LOG_DIR/test-last.log" "$LOG_DIR/test-$(echo "$name" | tr -c 'A-Za-z0-9' '-').log"; FAIL=$((FAIL+1)); fi
}
skip() { printf '  \033[33mSKIP\033[0m  %s - %s\n' "$1" "$2"; SKIP=$((SKIP+1)); }

say "Static checks"
for f in run.sh test.sh stop.sh setup-db.sh reset-demo.sh demo-sql.sh; do check "shell syntax $f" bash -n "$f"; done
check "every SQL script exists (00-15)" bash -c 'for n in 00_admin_setup 00_drop_all 01_tables 02_constraints 03_sequences 04_sample_data 05_queries 06_functions 07_procedures 08_transactions 09_triggers 10_cursors 11_exceptions 12_views 13_indexes 14_users_roles 15_post_setup; do test -s database/$n.sql || exit 1; done'

say "Backend unit tests (JUnit 5)"
if command -v mvn >/dev/null; then check "mvn test (mark formula, payroll, attendance, grades, Java syllabus)" bash -c 'cd backend && mvn -q -B test'
else skip "mvn test" "Maven not installed"; fi

say "Frontend"
if command -v npm >/dev/null; then
  check "npm install" bash -c 'cd frontend && npm install --no-audit --no-fund'
  check "npm run build" bash -c 'cd frontend && npm run build'
else skip "frontend build" "npm not installed"; fi

say "Running system (needs ./run.sh)"
if curl -fs http://localhost:8080/api/health 2>/dev/null | grep -q '"database":"UP"'; then
  check "API tests against Oracle (tests/api.test.mjs)" node --test tests/
else
  skip "API tests" "backend is not running or Oracle is down - start it with ./run.sh"
fi

echo
printf '  Result: %d passed, %d failed, %d skipped\n' "$PASS" "$FAIL" "$SKIP"
[ "$FAIL" -eq 0 ]
