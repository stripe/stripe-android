#!/usr/bin/env bash
set -euo pipefail

artifact_dir=${1:?Usage: build_firebase_test_lab_artifacts.sh ARTIFACT_DIR}
connections_backend_url=${STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL:?Missing Financial Connections backend URL}

if [[ -z "$artifact_dir" || "$artifact_dir" == "/" ]]; then
  echo "Refusing to use unsafe artifact directory: $artifact_dir" >&2
  exit 1
fi

mkdir -p "$artifact_dir"
rm -f "$artifact_dir"/*.apk

./gradlew \
  :connect:assembleDebugAndroidTest \
  :connect-example:assembleDebug \
  :connect-example:assembleDebugAndroidTest \
  :payment-method-messaging:assembleDebugAndroidTest \
  :payments-core:assembleDebugAndroidTest \
  :paymentsheet:assembleDebugAndroidTest \
  :paymentsheet-example:assembleBaseDebug \
  :paymentsheet-example:assembleBaseDebugAndroidTest \
  :financial-connections:assembleDebugAndroidTest \
  :financial-connections-example:assembleDebug \
  :financial-connections-example:assembleDebugAndroidTest \
  :financial-connections-example:assembleRelease \
  :crypto-onramp-example:assembleDebug \
  :crypto-onramp-example:assembleDebugAndroidTest \
  :camera-core:assembleDebugAndroidTest \
  :stripecardscan:assembleDebugAndroidTest \
  :stripecardscan-example:assembleDebug \
  :stripecardscan-example:assembleDebugAndroidTest \
  -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL="$connections_backend_url" \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_SENTRY_DSN="${STRIPE_PAYMENTSHEET_EXAMPLE_SENTRY_DSN:-}"

copy_single_apk() {
  local pattern=$1
  local destination=$2
  local matches=()

  while IFS= read -r match; do
    matches+=("$match")
  done < <(compgen -G "$pattern")

  if [[ ${#matches[@]} -ne 1 ]]; then
    echo "Expected one APK matching $pattern, found ${#matches[@]}" >&2
    printf '  %s\n' "${matches[@]}" >&2
    exit 1
  fi

  cp "${matches[0]}" "$artifact_dir/$destination"
}

copy_single_apk "connect/build/outputs/apk/androidTest/debug/*.apk" "connect-test.apk"
copy_single_apk "connect-example/build/outputs/apk/debug/*.apk" "connect-example.apk"
copy_single_apk "connect-example/build/outputs/apk/androidTest/debug/*.apk" "connect-example-test.apk"
copy_single_apk "payment-method-messaging/build/outputs/apk/androidTest/debug/*.apk" "payment-method-messaging-test.apk"
copy_single_apk "payments-core/build/outputs/apk/androidTest/debug/*.apk" "payments-core-test.apk"
copy_single_apk "paymentsheet/build/outputs/apk/androidTest/debug/*.apk" "paymentsheet-test.apk"
copy_single_apk "paymentsheet-example/build/outputs/apk/base/debug/*.apk" "paymentsheet-example.apk"
copy_single_apk "paymentsheet-example/build/outputs/apk/androidTest/base/debug/*.apk" "paymentsheet-example-test.apk"
copy_single_apk "financial-connections/build/outputs/apk/androidTest/debug/*.apk" "financial-connections-test.apk"
copy_single_apk "financial-connections-example/build/outputs/apk/debug/*.apk" "financial-connections-example.apk"
copy_single_apk \
  "financial-connections-example/build/outputs/apk/androidTest/debug/*.apk" \
  "financial-connections-example-test.apk"
copy_single_apk \
  "financial-connections-example/build/outputs/apk/release/*.apk" \
  "financial-connections-example-release.apk"
copy_single_apk "crypto-onramp-example/build/outputs/apk/debug/*.apk" "crypto-onramp-example.apk"
copy_single_apk "crypto-onramp-example/build/outputs/apk/androidTest/debug/*.apk" "crypto-onramp-example-test.apk"
copy_single_apk "camera-core/build/outputs/apk/androidTest/debug/*.apk" "camera-core-test.apk"
copy_single_apk "stripecardscan/build/outputs/apk/androidTest/debug/*.apk" "stripecardscan-test.apk"
copy_single_apk "stripecardscan-example/build/outputs/apk/debug/*.apk" "stripecardscan-example.apk"
copy_single_apk "stripecardscan-example/build/outputs/apk/androidTest/debug/*.apk" "stripecardscan-example-test.apk"

echo "Firebase Test Lab artifacts:"
ls -lh "$artifact_dir"
