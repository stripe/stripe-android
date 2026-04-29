#!/usr/bin/env bash

set -euo pipefail

RUNS="${1:-100}"
OUTPUT_CSV="${2:-paymentsheet-example/build/reports/payment-sheet-load-results.csv}"
APP_ID="${APP_ID:-com.stripe.android.paymentsheet.example}"
TEST_PACKAGE="${TEST_PACKAGE:-${APP_ID}.test}"
RUNNER="${RUNNER:-androidx.test.runner.AndroidJUnitRunner}"
TEST_CLASS="${TEST_CLASS:-com.stripe.android.lpm.PaymentSheetLoadTest}"
GRADLEW="${GRADLEW:-./gradlew}"
RESULT_TAG="${RESULT_TAG:-PaymentSheetLoadTest}"
RESULT_PREFIX="${RESULT_PREFIX:-RESULT,}"

ADB_ARGS=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB_ARGS=(-s "${ANDROID_SERIAL}")
fi

mkdir -p "$(dirname "${OUTPUT_CSV}")"

echo "Assembling and installing paymentsheet-example test APKs..."
"${GRADLEW}" \
  :paymentsheet-example:installBaseDebug \
  :paymentsheet-example:installBaseDebugAndroidTest

echo "run,test_name,duration_ms" > "${OUTPUT_CSV}"

for ((run = 1; run <= RUNS; run++)); do
  echo "Starting run ${run}/${RUNS}..."

  adb "${ADB_ARGS[@]}" logcat -c

  adb "${ADB_ARGS[@]}" shell am instrument -w \
    -e class "${TEST_CLASS}" \
    "${TEST_PACKAGE}/${RUNNER}" > /tmp/payment_sheet_load_benchmark_run_${run}.txt

  log_output="$(adb "${ADB_ARGS[@]}" logcat -d -s "${RESULT_TAG}:I" '*:S')"

  run_results="$(
    printf '%s\n' "${log_output}" |
      awk -v run="${run}" -v prefix="${RESULT_PREFIX}" '
        index($0, prefix) {
          split(substr($0, index($0, prefix) + length(prefix)), fields, ",")
          if (length(fields) >= 2) {
            printf "%s,%s,%s\n", run, fields[1], fields[2]
          }
        }
      '
  )"

  if [[ -z "${run_results}" ]]; then
    echo "No timing results found for run ${run}." >&2
    echo "Instrumentation output:" >&2
    cat /tmp/payment_sheet_load_benchmark_run_${run}.txt >&2
    exit 1
  fi

  printf '%s\n' "${run_results}" >> "${OUTPUT_CSV}"
done

echo "Wrote benchmark results to ${OUTPUT_CSV}"
