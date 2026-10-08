# Shared settings for all scripts (sourced, not executed). Change here if a port or password clashes.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export ORACLE_CONTAINER="${ORACLE_CONTAINER:-college-oracle}"
export ORACLE_IMAGE="${ORACLE_IMAGE:-gvenzl/oracle-free:23-slim-faststart}"
export ORACLE_SYS_PASSWORD="${ORACLE_SYS_PASSWORD:-oracle123}"
export ORACLE_SERVICE="${ORACLE_SERVICE:-FREEPDB1}"
export ORACLE_APP_USER="${ORACLE_USER:-college}"
export ORACLE_APP_PASSWORD="${ORACLE_PASSWORD:-college123}"
export ORACLE_PORT="${ORACLE_PORT:-1521}"
RUN_DIR="$ROOT/.run"
LOG_DIR="$ROOT/logs"
mkdir -p "$RUN_DIR" "$LOG_DIR" "$ROOT/output"

say()  { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
ok()   { printf '\033[1;32m  OK  \033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m WARN \033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m ERROR\033[0m %s\n' "$*" >&2; exit 1; }

# sqlplus_run <user> <password> : reads SQL from stdin and runs it inside the Oracle container
sqlplus_run() {
  docker exec -i "$ORACLE_CONTAINER" sqlplus -s -L "$1/$2@//localhost:1521/$ORACLE_SERVICE"
}
sys_sql()  { sqlplus_run system "$ORACLE_SYS_PASSWORD"; }
app_sql()  { sqlplus_run "$ORACLE_APP_USER" "$ORACLE_APP_PASSWORD"; }

oracle_up() {
  echo "SELECT 'READY' FROM dual;" | sys_sql 2>/dev/null | grep -q READY
}
