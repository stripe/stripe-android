#!/usr/bin/env bash
set -euo pipefail

artifact_dir=${1:?Usage: run_firebase_test_lab_suite.sh ARTIFACT_DIR RESULTS_DIR}
results_dir=${2:?Usage: run_firebase_test_lab_suite.sh ARTIFACT_DIR RESULTS_DIR}
project_id=${FTL_GOOGLE_CLOUD_PROJECT:?Missing FTL_GOOGLE_CLOUD_PROJECT}
results_bucket=${FTL_RESULTS_BUCKET:?Missing FTL_RESULTS_BUCKET}
credentials_file=${GOOGLE_APPLICATION_CREDENTIALS:?Missing GOOGLE_APPLICATION_CREDENTIALS}
device_spec=${FTL_TEST_DEVICES:-MediumPhone.arm,33,en_US,portrait}

if [[ ! -d "$artifact_dir" ]]; then
  echo "Missing Firebase Test Lab artifact directory: $artifact_dir" >&2
  exit 1
fi

if [[ -z "$results_dir" || "$results_dir" == "/" ]]; then
  echo "Refusing to use unsafe results directory: $results_dir" >&2
  exit 1
fi

if [[ "$results_bucket" != gs://* ]]; then
  echo "FTL_RESULTS_BUCKET must start with gs://: $results_bucket" >&2
  exit 1
fi

if [[ ! -f "$credentials_file" ]]; then
  echo "Missing Google application credentials: $credentials_file" >&2
  exit 1
fi

if ! command -v gcloud >/dev/null; then
  echo "The Google Cloud CLI is not installed" >&2
  exit 1
fi

IFS=',' read -r device_model device_version device_locale device_orientation <<< "$device_spec"
if [[ -z "$device_model" || -z "$device_version" || -z "$device_locale" || -z "$device_orientation" ]]; then
  echo "Invalid FTL_TEST_DEVICES value: $device_spec" >&2
  exit 1
fi

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
lifecycle_file="$script_dir/firebase_test_lab_bucket_lifecycle.json"
service_account=$(jq -r '.client_email // empty' "$credentials_file")
if [[ -z "$service_account" ]]; then
  echo "Google application credentials do not contain client_email" >&2
  exit 1
fi

mkdir -p "$results_dir/logs"
chmod 700 "$results_dir"

gcloud auth activate-service-account "$service_account" \
  --key-file="$credentials_file" \
  --project="$project_id" \
  --quiet

cleanup() {
  # Invoked by the EXIT trap.
  # shellcheck disable=SC2317
  gcloud auth revoke "$service_account" --quiet >/dev/null 2>&1 || true
}
trap cleanup EXIT

if ! gcloud storage buckets describe "$results_bucket" --project="$project_id" >/dev/null 2>&1; then
  echo "Creating Firebase Test Lab results bucket: $results_bucket"
  gcloud storage buckets create "$results_bucket" \
    --project="$project_id" \
    --location=us-central1 \
    --uniform-bucket-level-access \
    --public-access-prevention \
    --soft-delete-duration=0 \
    --lifecycle-file="$lifecycle_file"
fi

run_identifier=${BITRISE_BUILD_SLUG:-local-$(date -u +%Y%m%dT%H%M%SZ)}
run_identifier=${run_identifier//[^[:alnum:]._-]/-}
results_prefix="bitrise/$run_identifier"
declare -a test_names=()
declare -a test_pids=()

start_test() {
  local name=$1
  local test_apk=$2
  local app_apk=$3
  local model=$4
  local test_targets=$5
  local environment_variables=$6
  local auto_google_login=$7
  local log_file="$results_dir/logs/$name.log"
  local -a command=(
    gcloud firebase test android run
    --project="$project_id"
    --type=instrumentation
    --test="$test_apk"
    --device="model=$model,version=$device_version,locale=$device_locale,orientation=$device_orientation"
    --timeout=60m
    --num-flaky-test-attempts=2
    --results-bucket="$results_bucket"
    --results-dir="$results_prefix/$name"
    --results-history-name="Stripe Android Bitrise"
    --client-details="matrixLabel=$name"
    --quiet
  )

  if [[ ! -f "$test_apk" ]]; then
    echo "Missing test APK for $name: $test_apk" >&2
    exit 1
  fi

  # Android library instrumentation APKs in this repository target their own
  # package. Bitrise's Test Lab step accepted them without an app path, but the
  # gcloud CLI requires --app, so submit the self-instrumenting APK as both.
  if [[ -z "$app_apk" ]]; then
    app_apk=$test_apk
  fi

  if [[ ! -f "$app_apk" ]]; then
    echo "Missing app APK for $name: $app_apk" >&2
    exit 1
  fi
  command+=(--app="$app_apk")

  if [[ -n "$test_targets" ]]; then
    command+=(--test-targets="$test_targets")
  fi

  if [[ -n "$environment_variables" ]]; then
    command+=(--environment-variables="$environment_variables")
  fi

  if [[ "$auto_google_login" == "true" ]]; then
    command+=(--auto-google-login)
  else
    command+=(--no-auto-google-login)
  fi

  echo "Starting Firebase Test Lab matrix: $name"
  ("${command[@]}" >"$log_file" 2>&1) &
  test_names+=("$name")
  test_pids+=("$!")
}

start_test \
  connect-instrumentation \
  "$artifact_dir/connect-test.apk" \
  "" \
  "$device_model" \
  "" \
  "" \
  false

start_test \
  connect-example-instrumentation \
  "$artifact_dir/connect-example-test.apk" \
  "$artifact_dir/connect-example.apk" \
  "$device_model" \
  "" \
  "" \
  false

start_test \
  payment-method-messaging-instrumentation \
  "$artifact_dir/payment-method-messaging-test.apk" \
  "" \
  "$device_model" \
  "" \
  "" \
  false

start_test \
  payments-core-instrumentation \
  "$artifact_dir/payments-core-test.apk" \
  "" \
  "$device_model" \
  "" \
  "" \
  false

for shard_index in 0 1 2 3 4 5; do
  start_test \
    "paymentsheet-instrumentation-shard-$shard_index" \
    "$artifact_dir/paymentsheet-test.apk" \
    "" \
    "$device_model" \
    "" \
    "numShards=6,shardIndex=$shard_index" \
    false
done

start_test \
  financial-connections-instrumentation \
  "$artifact_dir/financial-connections-test.apk" \
  "" \
  "$device_model" \
  "" \
  "" \
  false

start_test \
  crypto-onramp-example-instrumentation \
  "$artifact_dir/crypto-onramp-example-test.apk" \
  "$artifact_dir/crypto-onramp-example.apk" \
  "$device_model" \
  "" \
  "" \
  false

start_test \
  camera-core-instrumentation \
  "$artifact_dir/camera-core-test.apk" \
  "" \
  "$device_model" \
  "" \
  "" \
  false

start_test \
  cardscan-instrumentation \
  "$artifact_dir/stripecardscan-test.apk" \
  "" \
  Pixel2.arm \
  "" \
  "" \
  false

for shard_index in 0 1 2 3 4 5; do
  shard_classes=$(python3 scripts/get_shard_test_classes.py \
    --shard-index "$shard_index" \
    --num-shards 6)
  shard_targets="class ${shard_classes//,/,class }"
  start_test \
    "paymentsheet-e2e-shard-$shard_index" \
    "$artifact_dir/paymentsheet-example-test.apk" \
    "$artifact_dir/paymentsheet-example.apk" \
    "$device_model" \
    "$shard_targets" \
    "" \
    true
done

start_test \
  paymentsheet-google-pay-e2e \
  "$artifact_dir/paymentsheet-example-test.apk" \
  "$artifact_dir/paymentsheet-example.apk" \
  "$device_model" \
  "class com.stripe.android.lpm.TestGooglePay" \
  "" \
  true

start_test \
  connect-e2e \
  "$artifact_dir/connect-example-e2e-test.apk" \
  "$artifact_dir/connect-example.apk" \
  "$device_model" \
  "class com.stripe.android.connect.example.e2e.AccountOnboardingTest" \
  "" \
  false

for scenario_index in 0 1 2 3 4 5 6; do
  start_test \
    "financial-connections-e2e-scenario-$scenario_index" \
    "$artifact_dir/financial-connections-example-test.apk" \
    "$artifact_dir/financial-connections-example.apk" \
    "$device_model" \
    "class com.stripe.android.financialconnections.example.FinancialConnectionsTestLabTest" \
    "scenarioIndex=$scenario_index" \
    false
done

overall_status=0
summary_file="$results_dir/summary.tsv"
printf 'test\tstatus\n' > "$summary_file"

for index in "${!test_pids[@]}"; do
  name=${test_names[$index]}
  pid=${test_pids[$index]}
  status=0
  if wait "$pid"; then
    result=passed
  else
    status=$?
    result="failed ($status)"
    overall_status=1
  fi

  printf '%s\t%s\n' "$name" "$result" | tee -a "$summary_file"
  echo "----- $name -----"
  cat "$results_dir/logs/$name.log"
done

mkdir -p "$results_dir/google-cloud"
if ! gcloud storage cp --recursive \
  "${results_bucket%/}/$results_prefix" \
  "$results_dir/google-cloud"; then
  echo "Failed to download Firebase Test Lab result artifacts" >&2
  overall_status=1
fi

exit "$overall_status"
