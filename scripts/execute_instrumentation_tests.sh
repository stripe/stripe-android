# Uncomment if tests are working locally but not in the github action. Keeping logcat output makes
# github perform slow (and can crash your browser) because of all of the logs and isn't necessary
# for debugging all issues.
# adb logcat -c
# adb logcat &

set -e # Fail on error.

# Exclude any modules with screenshot tests here. Then run them with the screenshot test package excluded.
./gradlew connectedAndroidTest -x :paymentsheet-example:connectedAndroidTest

if [ -d "$BITRISE_TEST_RESULT_DIR" ]; then
  mkdir -p "$BITRISE_TEST_RESULT_DIR/test_results"

  find . -type d -regex '.*/build/reports/androidTests/connected$' -exec cp -R {} $BITRISE_TEST_RESULT_DIR/test_results \;
fi
