#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.codex-runtime"
RUNLOG_DIR="$RUNTIME_DIR/runlogs"
PID_DIR="$RUNTIME_DIR/pids"
REDIS_DATA_DIR="$RUNTIME_DIR/redis-data"

BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"
REDIS_PID_FILE="$PID_DIR/redis.pid"

BACKEND_OUT_LOG="$RUNLOG_DIR/backend.out.log"
BACKEND_ERR_LOG="$RUNLOG_DIR/backend.err.log"
FRONTEND_OUT_LOG="$RUNLOG_DIR/frontend.out.log"
FRONTEND_ERR_LOG="$RUNLOG_DIR/frontend.err.log"
REDIS_LOG="$RUNLOG_DIR/redis.log"

JAVA_HOME_DEFAULT="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"

mkdir -p "$RUNLOG_DIR" "$PID_DIR" "$REDIS_DATA_DIR"

export LANG="${LANG:-C}"
export LC_ALL="${LC_ALL:-C}"

if [[ -d "$JAVA_HOME_DEFAULT" ]]; then
  export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

BIND_HOST="${BIND_HOST:-127.0.0.1}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
SERVER_PORT="${SERVER_PORT:-8080}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-course_platform_demo}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

VITE_API_BASE_URL="${VITE_API_BASE_URL:-http://$BIND_HOST:$SERVER_PORT/api/v1}"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing command: $cmd"
    exit 1
  fi
}

port_open() {
  local host="$1"
  local port="$2"
  nc -z "$host" "$port" >/dev/null 2>&1
}

wait_for_port() {
  local name="$1"
  local host="$2"
  local port="$3"
  local retries="${4:-60}"
  local delay="${5:-1}"

  for _ in $(seq 1 "$retries"); do
    if port_open "$host" "$port"; then
      return 0
    fi
    sleep "$delay"
  done

  echo "$name did not become ready on $host:$port"
  return 1
}

pid_is_alive() {
  local pid_file="$1"
  if [[ ! -f "$pid_file" ]]; then
    return 1
  fi

  local pid
  pid="$(<"$pid_file")"
  [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1
}

mysqladmin_ping() {
  local -a cmd=(mysqladmin ping -h "$DB_HOST" -P "$DB_PORT" "-u$DB_USERNAME")
  if [[ -n "$DB_PASSWORD" ]]; then
    cmd+=("-p$DB_PASSWORD")
  fi
  "${cmd[@]}" >/dev/null 2>&1
}

redis_cli_ping() {
  local -a cmd=(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT")
  if [[ -n "$REDIS_PASSWORD" ]]; then
    cmd+=(-a "$REDIS_PASSWORD")
  fi
  cmd+=(ping)
  "${cmd[@]}" >/dev/null 2>&1
}

ensure_mysql() {
  if mysqladmin_ping; then
    echo "MySQL is already running on $DB_HOST:$DB_PORT"
    return 0
  fi

  if [[ "$DB_HOST" != "127.0.0.1" && "$DB_HOST" != "localhost" ]]; then
    echo "MySQL is not reachable on $DB_HOST:$DB_PORT"
    echo "This script only auto-starts local Homebrew MySQL. Start your DB first."
    exit 1
  fi

  echo "Starting MySQL via Homebrew..."
  brew services start mysql >/dev/null
  wait_for_port "MySQL" "$DB_HOST" "$DB_PORT" 30 1
  echo "MySQL is ready"
}

ensure_redis() {
  if redis_cli_ping; then
    echo "Redis is already running on $REDIS_HOST:$REDIS_PORT"
    return 0
  fi

  if [[ "$REDIS_HOST" != "127.0.0.1" && "$REDIS_HOST" != "localhost" ]]; then
    echo "Redis is not reachable on $REDIS_HOST:$REDIS_PORT"
    echo "This script only auto-starts local Redis. Start your Redis first."
    exit 1
  fi

  echo "Starting Redis..."
  (
    export LANG=C
    export LC_ALL=C
    redis-server \
      --save '' \
      --appendonly no \
      --port "$REDIS_PORT" \
      --bind "$REDIS_HOST" \
      --dir "$REDIS_DATA_DIR" \
      --daemonize yes \
      --pidfile "$REDIS_PID_FILE" \
      --logfile "$REDIS_LOG"
  )

  wait_for_port "Redis" "$REDIS_HOST" "$REDIS_PORT" 15 1
  echo "Redis is ready"
}

ensure_frontend_deps() {
  local frontend_dir="$ROOT_DIR/frontend"

  if [[ ! -x "$frontend_dir/node_modules/.bin/vite" ]]; then
    echo "Installing frontend dependencies..."
    (cd "$frontend_dir" && npm install)
  fi
}

start_backend() {
  if pid_is_alive "$BACKEND_PID_FILE" || port_open "$BIND_HOST" "$SERVER_PORT"; then
    echo "Backend is already running on $BIND_HOST:$SERVER_PORT"
    return 0
  fi

  : >"$BACKEND_OUT_LOG"
  : >"$BACKEND_ERR_LOG"

  echo "Starting backend..."
  (
    cd "$ROOT_DIR/backend"
    export DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD
    export REDIS_HOST REDIS_PORT REDIS_PASSWORD
    export SERVER_PORT
    nohup mvn spring-boot:run >"$BACKEND_OUT_LOG" 2>"$BACKEND_ERR_LOG" &
    echo $! >"$BACKEND_PID_FILE"
  )

  wait_for_port "Backend" "$BIND_HOST" "$SERVER_PORT" 180 1
  echo "Backend is ready"
}

start_frontend() {
  if pid_is_alive "$FRONTEND_PID_FILE" || port_open "$BIND_HOST" "$FRONTEND_PORT"; then
    echo "Frontend is already running on $BIND_HOST:$FRONTEND_PORT"
    return 0
  fi

  : >"$FRONTEND_OUT_LOG"
  : >"$FRONTEND_ERR_LOG"

  echo "Starting frontend..."
  (
    cd "$ROOT_DIR/frontend"
    export VITE_API_BASE_URL
    nohup npm run dev -- --host "$BIND_HOST" --port "$FRONTEND_PORT" >"$FRONTEND_OUT_LOG" 2>"$FRONTEND_ERR_LOG" &
    echo $! >"$FRONTEND_PID_FILE"
  )

  wait_for_port "Frontend" "$BIND_HOST" "$FRONTEND_PORT" 60 1
  echo "Frontend is ready"
}

print_summary() {
  cat <<EOF

Services are up.

Frontend: http://$BIND_HOST:$FRONTEND_PORT
Backend:  http://$BIND_HOST:$SERVER_PORT

Environment:
- DB: $DB_USERNAME@$DB_HOST:$DB_PORT/$DB_NAME
- Redis: $REDIS_HOST:$REDIS_PORT
- VITE_API_BASE_URL: $VITE_API_BASE_URL

Logs:
- $BACKEND_OUT_LOG
- $BACKEND_ERR_LOG
- $FRONTEND_OUT_LOG
- $FRONTEND_ERR_LOG
- $REDIS_LOG
EOF
}

main() {
  require_cmd brew
  require_cmd mysqladmin
  require_cmd redis-server
  require_cmd redis-cli
  require_cmd mvn
  require_cmd npm
  require_cmd nc

  ensure_mysql
  ensure_redis
  ensure_frontend_deps
  start_backend
  start_frontend
  print_summary
}

main "$@"
