set -e

copy_test_results () {
  directories=$(find . -type d -regex "$1")
  for directory in $directories
  do
    relative_directory=$(python3 -c "import os.path; print(os.path.relpath('$directory', '.'))")
    destination_directory="/tmp/test_results/$relative_directory"
    mkdir -p "$destination_directory"
    cp -R "$relative_directory" "$destination_directory"
  done
}

# We won't always have test results, so put a blank file there so the upload won't fail.
mkdir -p "/tmp/test_results"
touch "/tmp/test_results/.keep"

copy_test_results ".*/build/reports/tests/testDebugUnitTest$"
copy_test_results ".*/build/reports/androidTests/connected$"

# If screenshots were requested, and it's a failure.
if [ "$INCLUDE_SCREENSHOT_ON_FAILURE" == "true" ] && [ "$BITRISE_BUILD_STATUS" == 1 ]; then
  copy_test_results ".*/screenshots/debug$"
fi

# If paparazzi screenshots were requested, and it's a failure.
if [ "$INCLUDE_PAPARAZZI_ON_FAILURE" == "true" ] && [ "$BITRISE_BUILD_STATUS" == 1 ]; then
  copy_test_results ".*/build/paparazzi/failures$"
fi

# If dependencies were requested, and it's a failure.
if [ "$INCLUDE_DEPENDENCIES_ON_FAILURE" == "true" ] && [ "$BITRISE_BUILD_STATUS" == 1 ]; then
  copy_test_results ".*/dependencies$"
fi
