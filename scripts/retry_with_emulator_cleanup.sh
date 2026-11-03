#!/bin/bash

# Retry a command, killing unhealthy emulators between attempts.
# Usage: ./scripts/retry_with_emulator_cleanup.sh <retries> <command...>

OUTPUT_LOG=$(mktemp)
trap 'rm -f "$OUTPUT_LOG"' EXIT

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
    "$@" 2>&1 | tee "$OUTPUT_LOG"
    local exit=${PIPESTATUS[0]}
    if [ $exit -eq 0 ]; then
      return 0
    fi
    count=$(($count + 1))
    if [ $count -lt $retries ]; then
      capture_attempt_results "$count"
      echo "Retry $count/$retries exited $exit. Checking for known failures..."
      clear_corrupted_orchestrator_cache
      echo "Checking emulator health..."
      kill_unhealthy_emulators
    else
      echo "Retry $count/$retries exited $exit, no more retries left."
      return $exit
    fi
  done
}

retry "$@"
