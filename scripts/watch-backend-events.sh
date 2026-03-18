#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_FILE="$ROOT_DIR/.codex-runtime/runlogs/backend.out.log"
TAIL_LINES="${TAIL_LINES:-20}"

if [[ ! -f "$LOG_FILE" ]]; then
  echo "Backend log file not found: $LOG_FILE"
  echo "Start the project first with ./scripts/start-dev.sh"
  exit 1
fi

echo "Watching backend course events from $LOG_FILE"
echo "Matched events:"
echo "- student submission"
echo "- peer evaluation"
echo "- teacher score"
echo

tail -n "$TAIL_LINES" -f "$LOG_FILE" | grep --line-buffered '\[COURSE_EVENT\]'
