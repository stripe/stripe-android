#!/usr/bin/env bash

set -euo pipefail

BASE_COMMIT="${BASE_COMMIT:-04d8b4fa7d35bf1d0004eae1b97b7e02c8284ac8}"
SLEEP_COMMIT="${SLEEP_COMMIT:-5f22d19360}"
RUNS_PER_CONDITION="${RUNS_PER_CONDITION:-4}"
ITERATIONS="${ITERATIONS:-20}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BENCHMARK_SCRIPT="${REPO_ROOT}/scripts/measure_latency_difference.rb"

TIMESTAMP="$(date +"%Y%m%d-%H%M%S")"
LOG_ROOT="${REPO_ROOT}/latency_experiment_logs/${TIMESTAMP}"
SUMMARY_FILE="${LOG_ROOT}/summary.txt"

mkdir -p "${LOG_ROOT}"

log() {
  printf '[%s] %s\n' "$(date +"%Y-%m-%d %H:%M:%S")" "$*"
}

run_case() {
  local label="$1"
  local base_commit="$2"
  local compare_commit="$3"
  local run_number="$4"

  local log_file="${LOG_ROOT}/${label}-run-${run_number}.log"

  log "Starting ${label} run ${run_number}/${RUNS_PER_CONDITION}"
  log "Base=${base_commit} Compare=${compare_commit}"
  log "Writing output to ${log_file}"

  {
    printf 'label=%s\n' "${label}"
    printf 'run_number=%s\n' "${run_number}"
    printf 'base_commit=%s\n' "${base_commit}"
    printf 'compare_commit=%s\n' "${compare_commit}"
    printf 'iterations=%s\n' "${ITERATIONS}"
    printf 'started_at=%s\n' "$(date +"%Y-%m-%d %H:%M:%S")"
    printf '\n'
  } | tee "${log_file}" >/dev/null

  if "${BENCHMARK_SCRIPT}" \
    --base-commit "${base_commit}" \
    --commit "${compare_commit}" \
    --iterations "${ITERATIONS}" 2>&1 | tee -a "${log_file}"; then
    local status="success"
    log "Completed ${label} run ${run_number}/${RUNS_PER_CONDITION}"
  else
    local status="failure"
    log "Failed ${label} run ${run_number}/${RUNS_PER_CONDITION}"
  fi

  {
    printf '\nfinished_at=%s\n' "$(date +"%Y-%m-%d %H:%M:%S")"
    printf 'status=%s\n' "${status}"
  } | tee -a "${log_file}" >/dev/null

  {
    printf '%s run %s: %s (%s)\n' "${label}" "${run_number}" "${status}" "${log_file}"
  } >> "${SUMMARY_FILE}"

  if [[ "${status}" != "success" ]]; then
    exit 1
  fi
}

{
  printf 'Latency experiment series\n'
  printf 'started_at=%s\n' "$(date +"%Y-%m-%d %H:%M:%S")"
  printf 'base_commit=%s\n' "${BASE_COMMIT}"
  printf 'sleep_commit=%s\n' "${SLEEP_COMMIT}"
  printf 'runs_per_condition=%s\n' "${RUNS_PER_CONDITION}"
  printf 'iterations=%s\n' "${ITERATIONS}"
  printf 'log_root=%s\n' "${LOG_ROOT}"
  printf '\n'
} > "${SUMMARY_FILE}"

log "Logs will be written under ${LOG_ROOT}"

for run_number in $(seq 1 "${RUNS_PER_CONDITION}"); do
  run_case "aa" "${BASE_COMMIT}" "${BASE_COMMIT}" "${run_number}"
  run_case "ab" "${BASE_COMMIT}" "${SLEEP_COMMIT}" "${run_number}"
done

{
  printf '\ncompleted_at=%s\n' "$(date +"%Y-%m-%d %H:%M:%S")"
} >> "${SUMMARY_FILE}"

log "All runs completed"
log "Summary: ${SUMMARY_FILE}"
