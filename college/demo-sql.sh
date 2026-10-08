#!/usr/bin/env bash
# Runs one DBMS demo script and shows Oracle's output, e.g.:   ./demo-sql.sh 05    or   ./demo-sql.sh 08_transactions
set -uo pipefail
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
[ $# -eq 1 ] || { echo "usage: ./demo-sql.sh <number|file-name>   (files are in database/)"; ls "$ROOT/database"; exit 1; }
file="$(ls "$ROOT"/database/"$1"*.sql 2>/dev/null | head -1)"
[ -n "$file" ] || die "No script starts with '$1' in database/"
echo "--- $(basename "$file") ---"
{ echo "SET SERVEROUTPUT ON SIZE UNLIMITED"; echo "SET LINESIZE 200"; echo "SET PAGESIZE 100"; cat "$file"; } | app_sql
