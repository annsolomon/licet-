#!/usr/bin/env bash
# Creates the COLLEGE schema in the Oracle container and loads tables, constraints, data, PL/SQL, views, roles.
#   ./setup-db.sh          create everything (safe to run again: it drops and recreates the COLLEGE objects)
set -uo pipefail
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
cd "$ROOT"

command -v docker >/dev/null || die "Docker is not installed. In GitHub Codespaces use the provided .devcontainer (it adds Docker)."
docker ps --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER" || die "Oracle container '$ORACLE_CONTAINER' is not running. Start it with ./run.sh"

say "Waiting for Oracle to accept connections"
for i in $(seq 1 90); do oracle_up && break; sleep 5; [ "$i" = 90 ] && die "Oracle did not become ready in 7.5 minutes"; done
ok "Oracle is ready"

run_script() {   # run_script <runner> <file>
  local runner="$1" file="$2"
  printf '  running %-26s' "$(basename "$file")"
  local out; out="$({ echo "SET SERVEROUTPUT ON SIZE UNLIMITED"; cat "$file"; } | "$runner" 2>&1)"
  local rc=$?
  if [ $rc -ne 0 ] || echo "$out" | grep -qE "^(ORA|SP2|PLS)-[0-9]+"; then
    echo "FAILED"; echo "$out" | tail -25; die "Script $(basename "$file") failed"
  fi
  echo "done"
}

say "Dropping old COLLEGE objects (if any)"
if echo "SELECT 'EXISTS' FROM all_users WHERE username='COLLEGE';" | sys_sql | grep -q EXISTS; then
  run_script app_sql database/00_drop_all.sql
fi

say "Creating schema and loading scripts"
run_script sys_sql database/00_admin_setup.sql
for f in 01_tables 02_constraints 03_sequences 04_sample_data 06_functions 07_procedures 09_triggers 10_cursors 11_exceptions 12_views 13_indexes; do
  run_script app_sql "database/$f.sql"
done
run_script sys_sql database/14_users_roles.sql
run_script app_sql database/15_post_setup.sql
# optional add-on: HOD / parent / advisor portals (240 students, 8-period attendance, absence notifications)
if [ -f database/16_portal_schema.sql ]; then
  for f in 16_portal_schema 17_portal_data 18_portal_trigger 19_portal_digest 20_portal_fix 21_portal_internals; do run_script app_sql "database/$f.sql"; done
fi

say "Checking that every PL/SQL object compiled"
bad="$(echo "SET HEADING OFF FEEDBACK OFF PAGESIZE 0
SELECT name || ' ' || type || ' line ' || line || ': ' || text FROM user_errors ORDER BY name, sequence;" | app_sql)"
if [ -n "$(echo "$bad" | tr -d '[:space:]')" ]; then echo "$bad"; die "PL/SQL compile errors (see above)"; fi
ok "No compile errors"

tables="$(echo "SET HEADING OFF FEEDBACK OFF PAGESIZE 0
SELECT COUNT(*) FROM user_tables;" | app_sql | tr -d '[:space:]')"
ok "Database ready: $tables tables, user '$ORACLE_APP_USER'"
