#!/usr/bin/env bash
# Puts the database back to the original demo data (drops everything in COLLEGE and reloads).
set -uo pipefail
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
say "Resetting demo data"
"$ROOT/setup-db.sh" && rm -f "$ROOT"/output/*.txt && ok "Demo data restored (report files removed)"
