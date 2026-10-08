#!/usr/bin/env bash
# Loads the portal tables, 240 students, parents, timetable, 8-period attendance and the absence trigger.
# Safe to run again: it first removes what it created before (without touching the rest of the database).
set -uo pipefail
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
cd "$ROOT"
docker ps --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER" || die "Oracle container is not running. Start it with ./run.sh"
echo "SELECT 'READY' FROM dual;" | sys_sql 2>/dev/null | grep -q READY || die "Oracle is not accepting connections yet"

has() { local o; o="$(echo "SELECT 'HAS' FROM user_objects WHERE object_name='$1';" | app_sql 2>&1)"; case "$o" in *HAS*) return 0 ;; *) return 1 ;; esac; }
if has NOTIFICATION; then
  if has INTERNALS_READY; then
    say "Portal data already present - rebuilding the whole database so it stays consistent"
    ./setup-db.sh || exit 1
    exit 0
  fi
  # data from an earlier add-on version: only run the scripts it is missing
  if has TIMETABLE_FIXED; then UPGRADE=21; elif has DIGEST_READY; then UPGRADE=20; else UPGRADE=19; fi
fi

run_script() {
  printf '  running %-26s' "$(basename "$2")"
  local out; out="$({ echo "SET SERVEROUTPUT ON SIZE UNLIMITED"; cat "$2"; } | "$1" 2>&1)"; local rc=$?
  if [ $rc -ne 0 ] || echo "$out" | grep -qE "^(ORA|SP2|PLS)-[0-9]+"; then echo FAILED; echo "$out" | tail -25; die "Script $(basename "$2") failed"; fi
  echo done
}
say "Loading portal schema and data"
case "${UPGRADE:-0}" in 21) LIST="21_portal_internals" ;; 20) LIST="20_portal_fix 21_portal_internals" ;; 19) LIST="19_portal_digest 20_portal_fix 21_portal_internals" ;; *) LIST="16_portal_schema 17_portal_data 18_portal_trigger 19_portal_digest 20_portal_fix 21_portal_internals" ;; esac
for f in $LIST; do run_script app_sql "database/$f.sql"; done
bad="$(echo "SET HEADING OFF FEEDBACK OFF PAGESIZE 0
SELECT name || ' ' || type || ' line ' || line || ': ' || text FROM user_errors ORDER BY name, sequence;" | app_sql)"
[ -z "$(echo "$bad" | tr -d '[:space:]')" ] || { echo "$bad"; die "PL/SQL compile errors"; }
echo "SET HEADING OFF FEEDBACK OFF PAGESIZE 0
SELECT (SELECT COUNT(*) FROM student) || ' students, ' || (SELECT COUNT(*) FROM attendance_record) || ' attendance records, ' || (SELECT COUNT(*) FROM notification) || ' notifications' FROM dual;" | app_sql
ok "Portal database ready"
