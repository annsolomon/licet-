#!/usr/bin/env bash
# Stops the backend and the frontend.   ./stop.sh --db   also stops the Oracle container.
source "$(cd "$(dirname "$0")" && pwd)/.env.sh"
QUIET=0; DB=0
for a in "$@"; do [ "$a" = "--quiet" ] && QUIET=1; [ "$a" = "--db" ] && DB=1; done
stop_tree() {   # stop_tree <pidfile> <port>
  if [ -f "$1" ]; then
    pid="$(cat "$1")"
    pkill -P "$pid" 2>/dev/null; kill "$pid" 2>/dev/null
    rm -f "$1"
  fi
  # whatever still listens on the port (mvn spawns a child JVM)
  if command -v lsof >/dev/null; then lsof -ti tcp:"$2" 2>/dev/null | xargs -r kill 2>/dev/null; fi
}
stop_tree "$RUN_DIR/backend.pid" 8080
stop_tree "$RUN_DIR/frontend.pid" 5173
[ "$QUIET" = 1 ] || ok "backend and frontend stopped"
if [ "$DB" = 1 ]; then docker stop "$ORACLE_CONTAINER" >/dev/null 2>&1 && ok "Oracle container stopped"; fi
