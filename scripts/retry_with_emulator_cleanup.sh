#!/bin/bash

# Retry a command, killing unhealthy emulators between attempts.
# Usage: ./scripts/retry_with_emulator_cleanup.sh [--fail-fast-tests] <retries> <command...>

FAIL_FAST_TESTS=false
if [ "${1:-}" = "--fail-fast-tests" ]; then
  FAIL_FAST_TESTS=true
  shift
fi

FAIL_FAST_GRACE_SECONDS="${BITRISE_FAIL_FAST_GRACE_SECONDS:-10}"

OUTPUT_LOG=$(mktemp)
OUTPUT_PIPE=""
cleanup_retry_files() {
  rm -f "$OUTPUT_LOG"
  if [ -n "$OUTPUT_PIPE" ]; then
    rm -f "$OUTPUT_PIPE"
  fi
}
trap cleanup_retry_files EXIT

SOURCE_DIR="${BITRISE_SOURCE_DIR:-.}"
SOURCE_DIR=$(cd "$SOURCE_DIR" && pwd)
RETRY_RESULTS_DIR="${BITRISE_RETRY_RESULTS_DIR:-/tmp/test_results/retry-results}"
RETRY_RUN_DIR=""

# Gradle emits this line after a test task has produced its JUnit failure report.
# Keep the detector narrow so compilation, setup, and emulator failures are not
# treated as test failures.
function is_gradle_test_failure {
  local log_file="$1"
  grep -qF "There were failing tests." "$log_file"
}

function process_is_running {
  local pid="$1"
  if [ -r "/proc/$pid/stat" ]; then
    local state
    state=$(awk '{print $3}' "/proc/$pid/stat" 2>/dev/null)
    [ -n "$state" ] && [ "$state" != "Z" ]
  else
    jobs -pr | grep -qE "(^|[[:space:]])$pid($|[[:space:]])"
  fi
}

function stop_process_group {
  local pid="$1"
  echo "Stopping failed Gradle test process group $pid..."
  kill -TERM -- "-$pid" 2>/dev/null || true
  kill -TERM "$pid" 2>/dev/null || true

  local remaining=$((FAIL_FAST_GRACE_SECONDS * 10))
  while process_is_running "$pid" && [ "$remaining" -gt 0 ]; do
    sleep 0.1
    remaining=$((remaining - 1))
  done

  if process_is_running "$pid"; then
    echo "Gradle test process group $pid did not stop gracefully; sending SIGKILL."
    kill -KILL -- "-$pid" 2>/dev/null || true
    kill -KILL "$pid" 2>/dev/null || true
  fi
}

function stop_gradle_daemons {
  local command_path="$1"
  case "$(basename "$command_path")" in
    gradlew|gradlew.bat)
      echo "Stopping Gradle daemons after early test termination..."
      if command -v timeout >/dev/null 2>&1; then
        timeout 30 "$command_path" --stop >/dev/null 2>&1 || true
      elif command -v gtimeout >/dev/null 2>&1; then
        gtimeout 30 "$command_path" --stop >/dev/null 2>&1 || true
      else
        "$command_path" --stop >/dev/null 2>&1 || true
      fi
      ;;
  esac
}

function run_with_live_test_failure_detection {
  : > "$OUTPUT_LOG"
  OUTPUT_PIPE=$(mktemp)
  rm -f "$OUTPUT_PIPE"
  mkfifo "$OUTPUT_PIPE"
  tee "$OUTPUT_LOG" < "$OUTPUT_PIPE" &
  local tee_pid=$!
  if command -v setsid >/dev/null 2>&1; then
    setsid "$@" > "$OUTPUT_PIPE" 2>&1 &
  else
    # macOS does not ship setsid. Job control gives this background command a
    # process group so local tests exercise the same group-kill contract.
    set -m
    "$@" > "$OUTPUT_PIPE" 2>&1 &
  fi
  local command_pid=$!

  while process_is_running "$command_pid"; do
    if is_gradle_test_failure "$OUTPUT_LOG"; then
      echo "Detected a Gradle test failure before the attempt completed."
      stop_process_group "$command_pid"
      stop_gradle_daemons "$1"
      wait "$command_pid" 2>/dev/null || true
      wait "$tee_pid" 2>/dev/null || true
      rm -f "$OUTPUT_PIPE"
      OUTPUT_PIPE=""
      return 143
    fi
    sleep 0.1
  done

  local command_status
  wait "$command_pid"
  command_status=$?
  wait "$tee_pid" 2>/dev/null || true
  rm -f "$OUTPUT_PIPE"
  OUTPUT_PIPE=""
  return "$command_status"
}

function capture_attempt_results {
  local attempt="$1"

  if [ ! -d "$SOURCE_DIR" ]; then
    echo "Cannot capture retry results because source directory is missing: $SOURCE_DIR" >&2
    return 0
  fi

  if [ -z "$RETRY_RUN_DIR" ]; then
    mkdir -p "$RETRY_RESULTS_DIR"
    RETRY_RUN_DIR=$(mktemp -d "$RETRY_RESULTS_DIR/run-XXXXXX")
  fi

  local attempt_dir="$RETRY_RUN_DIR/attempt-$attempt"
  local captured=0
  while IFS= read -r -d '' result_dir; do
    local relative="${result_dir#"$SOURCE_DIR"/}"
    local destination="$attempt_dir/$relative"
    mkdir -p "$(dirname "$destination")"
    cp -R "$result_dir" "$destination"
    captured=$((captured + 1))
    echo "Captured retry $attempt results: $relative"
  done < <(
    find "$SOURCE_DIR" -type d \( \
      -path '*/build/outputs/androidTest-results/connected' -o \
      -path '*/build/outputs/androidTest-results/managedDevice' \
    \) -print0
  )

  if [ "$captured" -eq 0 ]; then
    echo "No instrumentation result directories found after retry $attempt."
  fi
}

function clear_corrupted_orchestrator_cache {
  if [ -f "$OUTPUT_LOG" ] && grep -qE "Failed to install split APK|Invalid File.*orchestrator" "$OUTPUT_LOG"; then
    echo "Detected orchestrator APK installation failure. Clearing corrupted cache..."
    rm -rf \
      ~/.gradle/caches/modules-2/files-2.1/androidx.test/orchestrator/ \
      ~/.gradle/caches/modules-2/metadata-*/descriptors/androidx.test/orchestrator/
    if [ -f ./gradlew ]; then
      ./gradlew --stop
    fi
    echo "Orchestrator cache cleared. Will re-download on next attempt."
  fi
}

function kill_unhealthy_emulators {
  local killed=0
  for device in $(adb devices | grep emulator | cut -f1); do
    if ! adb -s "$device" shell "service check package" 2>/dev/null | grep -q "Service package: found"; then
      echo "Emulator $device has no package service, killing..."
      adb -s "$device" emu kill 2>/dev/null || true
      killed=$((killed + 1))
    fi
  done
  echo "Killed $killed unhealthy emulator(s)."
  if [ $killed -gt 0 ]; then
    sleep 5
  fi
}

function retry {
  local retries=$1
  shift

  local count=0
  while true; do
    local exit
    if [ "$FAIL_FAST_TESTS" = true ] && [ $((count + 1)) -lt "$retries" ]; then
      run_with_live_test_failure_detection "$@"
      exit=$?
    else
      "$@" 2>&1 | tee "$OUTPUT_LOG"
      exit=${PIPESTATUS[0]}
    fi
    if [ "$exit" -eq 0 ]; then
      return 0
    fi
    count=$((count + 1))
    if [ "$count" -lt "$retries" ]; then
      capture_attempt_results "$count"
      echo "Retry $count/$retries exited $exit. Checking for known failures..."
      clear_corrupted_orchestrator_cache
      echo "Checking emulator health..."
      kill_unhealthy_emulators
    else
      echo "Retry $count/$retries exited $exit, no more retries left."
      return "$exit"
    fi
  done
}

retry "$@"
