#!/usr/bin/env bash
set -o pipefail
set -e

# Maestro tags to be executed. see https://maestro.mobile.dev/cli/tags
MAESTRO_TAGS=""

while getopts ":t:" opt; do
  case $opt in
    t) MAESTRO_TAGS="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    exit 1
    ;;
  esac
done

# Check if tags is empty
if [ -z "$MAESTRO_TAGS" ]
then
  echo "Tags parameter is required."
  exit 1
fi

echo "test_environment: $test_environment"
echo "MAESTRO_TAGS: $MAESTRO_TAGS"

export MAESTRO_VERSION=1.38.1

# Retry mechanism for Maestro installation
MAX_RETRIES=5
RETRY_COUNT=0

while [ "$RETRY_COUNT" -lt "$MAX_RETRIES" ]; do
    curl -Ls "https://get.maestro.mobile.dev" | bash &&
    export PATH="$PATH:$HOME/.maestro/bin" &&
    maestro -v &&
    break

    let RETRY_COUNT=RETRY_COUNT+1
    echo "Attempt $RETRY_COUNT failed. Retrying..."
    sleep 5
done

if [ "$RETRY_COUNT" -eq "$MAX_RETRIES" ]; then
    echo "Installation failed after $MAX_RETRIES attempts."
    exit 1
fi

# install APK.
adb install $BITRISE_APK_PATH

# Function to check if a YAML file contains a specific tag
contains_tag() {
    local file="$1"
    local tag="$2"
    awk '/tags:/,/---/' "$file" | grep -q "$tag"
}

TEST_DIR_PATH=maestro/financial-connections
TEST_RESULTS_PATH=/tmp/test_results
RETRY_COUNT=0

# Create test results folder.
mkdir -p $TEST_RESULTS_PATH

for TEST_FILE_PATH in "$TEST_DIR_PATH"/*.yaml; do
  # Check if MAESTRO_TAGS are present in the test file
  if contains_tag "$TEST_FILE_PATH" "$MAESTRO_TAGS"; then
    # Just run the test if it's tagged as edge, if on an edge environment
    if [ "$test_environment" != "edge" ] || contains_tag "$TEST_FILE_PATH" "edge"; then
      # Execute Maestro test flow and retry if failed
      while [ "$RETRY_COUNT" -lt "$MAX_RETRIES" ]; do
        maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output $TEST_RESULTS_PATH/$(basename "$TEST_FILE_PATH").xml "$TEST_FILE_PATH" && break
        let RETRY_COUNT=RETRY_COUNT+1
        echo "Maestro test attempt $RETRY_COUNT failed. Retrying..."
      done
      if [ "$RETRY_COUNT" -eq "$MAX_RETRIES" ]; then
        echo "Maestro tests failed after $MAX_RETRIES attempts."
        exit 1
      else
        RETRY_COUNT=0
      fi
    else
      echo "Skipping test file without 'edge' tag: $TEST_FILE_PATH"
    fi
  fi
done
