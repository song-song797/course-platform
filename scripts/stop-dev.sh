#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.codex-runtime"
PID_DIR="$RUNTIME_DIR/pids"

BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"
REDIS_PID_FILE="$PID_DIR/redis.pid"

WITH_MYSQL=0

if [[ "${1:-}" == "--with-mysql" ]]; then
  WITH_MYSQL=1
fi

pid_is_alive() {
  local pid_file="$1"
  if [[ ! -f "$pid_file" ]]; then
    return 1
  fi

  local pid
  pid="$(<"$pid_file")"
  [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1
}

stop_by_pid_file() {
  local name="$1"
  local pid_file="$2"

  if ! pid_is_alive "$pid_file"; then
    rm -f "$pid_file"
    echo "$name is not running from managed pid file"
    return 0
  fi

  local pid
  pid="$(<"$pid_file")"
  echo "Stopping $name (pid $pid)..."
  kill "$pid" >/dev/null 2>&1 || true

  for _ in $(seq 1 15); do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      rm -f "$pid_file"
      echo "$name stopped"
      return 0
    fi
    sleep 1
  done

  echo "$name did not stop gracefully, forcing stop..."
  kill -9 "$pid" >/dev/null 2>&1 || true
  rm -f "$pid_file"
  echo "$name stopped"
}

maybe_stop_mysql() {
  if [[ "$WITH_MYSQL" -ne 1 ]]; then
    echo "MySQL was left running. Use ./scripts/stop-dev.sh --with-mysql if you also want to stop it."
    return 0
  fi

  if ! command -v brew >/dev/null 2>&1; then
    echo "brew not found, skipping MySQL stop"
    return 0
  fi

  echo "Stopping MySQL via Homebrew..."
  brew services stop mysql >/dev/null || true
  echo "MySQL stop requested"
}

main() {
  stop_by_pid_file "Frontend" "$FRONTEND_PID_FILE"
  stop_by_pid_file "Backend" "$BACKEND_PID_FILE"
  stop_by_pid_file "Redis" "$REDIS_PID_FILE"
  maybe_stop_mysql
}

main "$@"
