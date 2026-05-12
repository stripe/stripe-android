#!/usr/bin/env bash
set -o pipefail

ITERATIONS=10
MODULE="financial-connections"
TAGS=("testmode-payments" "testmode-data" "testmode-token")
RESULTS_DIR="deflake-results"

mkdir -p "$RESULTS_DIR"

echo "Building and installing financial-connections-example..."
./gradlew :financial-connections-example:assembleDebug || { echo "Build failed"; exit 1; }

APK_PATH=$(find financial-connections-example/build/outputs/apk/debug -name "*.apk" | head -1)
if [ -z "$APK_PATH" ]; then
  echo "APK not found after build"
  exit 1
fi

adb install -r "$APK_PATH" || { echo "Install failed"; exit 1; }
export BITRISE_APK_PATH="$APK_PATH"

echo "App installed successfully."
echo ""

for tag in "${TAGS[@]}"; do
  pass=0
  fail=0
  log_file="$RESULTS_DIR/${tag}.log"
  : > "$log_file"

  echo "========================================="
  echo "Running $tag ($ITERATIONS iterations)"
  echo "========================================="

  for i in $(seq 1 $ITERATIONS); do
    echo -n "[$tag] Run $i/$ITERATIONS... "
    if bash ./scripts/execute_maestro_tests.sh -m "$MODULE" -t "$tag" >> "$log_file" 2>&1; then
      echo "PASS"
      ((pass++))
    else
      echo "FAIL"
      ((fail++))
    fi
  done

  rate=$(echo "scale=1; $pass * 100 / $ITERATIONS" | bc)
  echo ""
  echo "[$tag] Results: $pass/$ITERATIONS passed ($rate%)"
  echo "[$tag] Failures: $fail"
  echo "[$tag] Full log: $log_file"
  echo ""
done

echo "========================================="
echo "Summary"
echo "========================================="
for tag in "${TAGS[@]}"; do
  pass=$(grep -c "^PASS" "$RESULTS_DIR/${tag}.log" 2>/dev/null || true)
  # Re-count from the script output isn't reliable from logs, so just point to the log
  echo "  $tag -> see $RESULTS_DIR/${tag}.log"
done
