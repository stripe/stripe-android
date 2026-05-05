#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BENCHMARK_SCRIPT="${REPO_ROOT}/scripts/measure_latency_difference.rb"

RUNS_PER_SCENARIO="${RUNS_PER_SCENARIO:-5}"
ITERATIONS="${ITERATIONS:-20}"
BASE_COMMIT="${BASE_COMMIT:-$(git -C "${REPO_ROOT}" rev-parse HEAD^)}"
SLEEP_COMMIT="${SLEEP_COMMIT:-$(git -C "${REPO_ROOT}" rev-parse HEAD)}"

TIMESTAMP="$(date +"%Y%m%d-%H%M%S")"
LOG_ROOT="${REPO_ROOT}/latency_benchmark_sensitivity_logs/${TIMESTAMP}"
SUMMARY_FILE="${LOG_ROOT}/summary.txt"
CSV_FILE="${LOG_ROOT}/results.csv"

mkdir -p "${LOG_ROOT}"

log() {
  printf '[%s] %s\n' "$(date +"%Y-%m-%d %H:%M:%S")" "$*"
}

progress() {
  local completed_runs="$1"
  local total_runs="$2"
  local scenario="$3"
  local run_number="$4"

  log "Progress ${completed_runs}/${total_runs}: running ${scenario} iteration ${run_number}/${RUNS_PER_SCENARIO}"
}

parse_log() {
  local scenario="$1"
  local run_number="$2"
  local log_file="$3"

  ruby - "${scenario}" "${run_number}" "${log_file}" >> "${CSV_FILE}" <<'RUBY'
scenario = ARGV[0]
run_number = ARGV[1]
log_file = ARGV[2]
in_report = false
rows = []

File.foreach(log_file) do |line|
  if line.include?('LATENCY DELTA REPORT (vs Base Commit)')
    in_report = true
    next
  end

  next unless in_report
  next if line.strip.empty? || line.start_with?('Test') || line.start_with?('-')
  next unless line.include?('|')

  columns = line.split('|').map(&:strip)
  next unless columns.length >= 5

  test_name = columns[0]
  significance = columns[4]
  next if test_name.empty?

  rows << [scenario, run_number, test_name, significance.start_with?('Yes') ? '1' : '0']
end

rows.each do |row|
  puts row.join(',')
end
RUBY
}

run_case() {
  local scenario="$1"
  local base_commit="$2"
  local compare_commit="$3"
  local run_number="$4"
  local completed_runs_before="$5"

  local log_file="${LOG_ROOT}/${scenario}-run-${run_number}.log"
  local total_runs=$((RUNS_PER_SCENARIO * 2))

  progress "$((completed_runs_before + 1))" "${total_runs}" "${scenario}" "${run_number}"

  log "Starting ${scenario} run ${run_number}/${RUNS_PER_SCENARIO}"
  log "Base=${base_commit} Compare=${compare_commit}"
  log "Writing output to ${log_file}"

  {
    printf 'scenario=%s\n' "${scenario}"
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
    log "Completed ${scenario} run ${run_number}/${RUNS_PER_SCENARIO}"
  else
    log "Failed ${scenario} run ${run_number}/${RUNS_PER_SCENARIO}"
    exit 1
  fi

  {
    printf '\nfinished_at=%s\n' "$(date +"%Y-%m-%d %H:%M:%S")"
    printf 'status=success\n'
  } | tee -a "${log_file}" >/dev/null

  parse_log "${scenario}" "${run_number}" "${log_file}"
}

{
  printf 'scenario,run_number,test_name,significant\n'
} > "${CSV_FILE}"

{
  printf 'Latency benchmark sensitivity experiment\n'
  printf 'started_at=%s\n' "$(date +"%Y-%m-%d %H:%M:%S")"
  printf 'base_commit=%s\n' "${BASE_COMMIT}"
  printf 'sleep_commit=%s\n' "${SLEEP_COMMIT}"
  printf 'runs_per_scenario=%s\n' "${RUNS_PER_SCENARIO}"
  printf 'iterations=%s\n' "${ITERATIONS}"
  printf 'log_root=%s\n' "${LOG_ROOT}"
  printf '\n'
} > "${SUMMARY_FILE}"

log "Logs will be written under ${LOG_ROOT}"
log "Plan: ${RUNS_PER_SCENARIO} same_commit run(s), then ${RUNS_PER_SCENARIO} sleep_commit run(s)"

completed_runs=0

for run_number in $(seq 1 "${RUNS_PER_SCENARIO}"); do
  run_case "same_commit" "${BASE_COMMIT}" "${BASE_COMMIT}" "${run_number}" "${completed_runs}"
  completed_runs=$((completed_runs + 1))
done

for run_number in $(seq 1 "${RUNS_PER_SCENARIO}"); do
  run_case "sleep_commit" "${BASE_COMMIT}" "${SLEEP_COMMIT}" "${run_number}" "${completed_runs}"
  completed_runs=$((completed_runs + 1))
done

ruby - "${CSV_FILE}" "${SUMMARY_FILE}" "${RUNS_PER_SCENARIO}" <<'RUBY'
csv_file = ARGV[0]
summary_file = ARGV[1]
runs_per_scenario = ARGV[2].to_i

rows = File.readlines(csv_file, chomp: true).drop(1).map do |line|
  scenario, run_number, test_name, significant = line.split(',', 4)
  {
    scenario: scenario,
    run_number: run_number.to_i,
    test_name: test_name,
    significant: significant == '1'
  }
end

scenario_names = {
  'same_commit' => 'False positive check (base vs base)',
  'sleep_commit' => 'Detection check (base vs sleep)'
}

File.open(summary_file, 'a') do |file|
  scenario_names.each do |scenario_key, title|
    scenario_rows = rows.select { |row| row[:scenario] == scenario_key }
    by_run = scenario_rows.group_by { |row| row[:run_number] }
    by_test = scenario_rows.group_by { |row| row[:test_name] }

    runs_with_any_significant = by_run.count { |_, run_rows| run_rows.any? { |row| row[:significant] } }
    runs_with_all_significant = by_run.count { |_, run_rows| run_rows.all? { |row| row[:significant] } }

    file.puts title
    file.puts "runs_with_any_significant=#{runs_with_any_significant}/#{runs_per_scenario}"
    file.puts "runs_with_all_significant=#{runs_with_all_significant}/#{runs_per_scenario}"
    file.puts 'per_test_significant_runs:'

    by_test.keys.sort.each do |test_name|
      significant_runs = by_test[test_name].count { |row| row[:significant] }
      file.puts "  #{test_name}=#{significant_runs}/#{runs_per_scenario}"
    end

    file.puts
  end

  file.puts "completed_at=#{Time.now.strftime('%Y-%m-%d %H:%M:%S')}"
  file.puts "csv_results=#{csv_file}"
end
RUBY

log "All runs completed"
log "Summary: ${SUMMARY_FILE}"
log "CSV: ${CSV_FILE}"
