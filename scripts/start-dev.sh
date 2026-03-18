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

export LANG="${LANG:-en_US.UTF-8}"
export LC_ALL="${LC_ALL:-en_US.UTF-8}"

if [[ -d "$JAVA_HOME_DEFAULT" ]]; then
  export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing command: $cmd"
    exit 1
  fi
}

port_open() {
  local port="$1"
  nc -z 127.0.0.1 "$port" >/dev/null 2>&1
}

wait_for_port() {
  local name="$1"
  local port="$2"
  local retries="${3:-60}"
  local delay="${4:-1}"

  for _ in $(seq 1 "$retries"); do
    if port_open "$port"; then
      return 0
    fi
    sleep "$delay"
  done

  echo "$name did not become ready on port $port"
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

ensure_mysql() {
  if mysqladmin ping -h 127.0.0.1 -P 3306 -uroot >/dev/null 2>&1; then
    echo "MySQL is already running on 127.0.0.1:3306"
    return 0
  fi

  echo "Starting MySQL via Homebrew..."
  brew services start mysql >/dev/null
  wait_for_port "MySQL" 3306 30 1
  echo "MySQL is ready"
}

ensure_redis() {
  if redis-cli ping >/dev/null 2>&1; then
    echo "Redis is already running on 127.0.0.1:6379"
    return 0
  fi

  echo "Starting Redis..."
  redis-server \
    --save '' \
    --appendonly no \
    --port 6379 \
    --bind 127.0.0.1 \
    --dir "$REDIS_DATA_DIR" \
    --daemonize yes \
    --pidfile "$REDIS_PID_FILE" \
    --logfile "$REDIS_LOG"

  wait_for_port "Redis" 6379 15 1
  echo "Redis is ready"
}

ensure_frontend_deps() {
  local frontend_dir="$ROOT_DIR/frontend"

  if [[ ! -d "$frontend_dir/node_modules" || ! -d "$frontend_dir/node_modules/@rolldown/binding-darwin-arm64" ]]; then
    echo "Installing frontend dependencies..."
    (cd "$frontend_dir" && npm install)
  fi

  if [[ -f "$frontend_dir/node_modules/.bin/vite" ]]; then
    chmod +x "$frontend_dir/node_modules/.bin/vite"
  fi
  if [[ -f "$frontend_dir/node_modules/vite/bin/vite.js" ]]; then
    chmod +x "$frontend_dir/node_modules/vite/bin/vite.js"
  fi
}

start_backend() {
  if pid_is_alive "$BACKEND_PID_FILE" || port_open 8080; then
    echo "Backend is already running on 127.0.0.1:8080"
    return 0
  fi

  echo "Starting backend..."
  (
    cd "$ROOT_DIR/backend"
    export DB_PASSWORD=''
    nohup mvn spring-boot:run >"$BACKEND_OUT_LOG" 2>"$BACKEND_ERR_LOG" &
    echo $! >"$BACKEND_PID_FILE"
  )

  wait_for_port "Backend" 8080 180 1
  echo "Backend is ready"
}

start_frontend() {
  if pid_is_alive "$FRONTEND_PID_FILE" || port_open 5173; then
    echo "Frontend is already running on 127.0.0.1:5173"
    return 0
  fi

  echo "Starting frontend..."
  (
    cd "$ROOT_DIR/frontend"
    nohup npm run dev -- --host 127.0.0.1 --port 5173 >"$FRONTEND_OUT_LOG" 2>"$FRONTEND_ERR_LOG" &
    echo $! >"$FRONTEND_PID_FILE"
  )

  wait_for_port "Frontend" 5173 60 1
  echo "Frontend is ready"
}

print_summary() {
  cat <<EOF

Services are up.

Frontend: http://127.0.0.1:5173
Backend:  http://127.0.0.1:8080

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
