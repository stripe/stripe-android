#!/bin/bash

# Retry a command, excluding completed instrumentation tests and cleaning up unhealthy emulators.
# Usage: ./scripts/retry_with_emulator_cleanup.sh <retries> <command...>

OUTPUT_LOG=$(mktemp)
export STRIPE_INSTRUMENTATION_RETRY_STATE=$(mktemp)
RETRY_HELPER="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/instrumentation_test_retry.py"
trap 'rm -f "$OUTPUT_LOG" "$STRIPE_INSTRUMENTATION_RETRY_STATE"' EXIT

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
    python3 "$RETRY_HELPER" snapshot "$STRIPE_INSTRUMENTATION_RETRY_STATE" "$@" || return 1
    "$@" 2>&1 | tee "$OUTPUT_LOG"
    local exit=${PIPESTATUS[0]}
    python3 "$RETRY_HELPER" update "$STRIPE_INSTRUMENTATION_RETRY_STATE" || return 1
    if [ $exit -eq 0 ]; then
      python3 "$RETRY_HELPER" finish "$STRIPE_INSTRUMENTATION_RETRY_STATE" || return 1
      return 0
    fi
    count=$(($count + 1))
    if [ $count -lt $retries ]; then
      echo "Retry $count/$retries exited $exit. Checking for known failures..."
      clear_corrupted_orchestrator_cache
      echo "Checking emulator health..."
      kill_unhealthy_emulators
    else
      echo "Retry $count/$retries exited $exit, no more retries left."
      python3 "$RETRY_HELPER" finish "$STRIPE_INSTRUMENTATION_RETRY_STATE" || return 1
      return $exit
    fi
  done
}

retry "$@"
