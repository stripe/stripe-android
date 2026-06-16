#!/bin/bash

# Retry a command, killing unhealthy emulators between attempts.
# Usage: ./scripts/retry_with_emulator_cleanup.sh <retries> <command...>

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
  until "$@"; do
    exit=$?
    count=$(($count + 1))
    if [ $count -lt $retries ]; then
      echo "Retry $count/$retries exited $exit. Checking emulator health..."
      kill_unhealthy_emulators
    else
      echo "Retry $count/$retries exited $exit, no more retries left."
      return $exit
    fi
  done
  return 0
}

retry "$@"
