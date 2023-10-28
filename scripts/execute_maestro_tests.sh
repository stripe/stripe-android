#!/usr/bin/env bash
set -o pipefail
set -x
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

export MAESTRO_VERSION=1.33.1

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

TEST_DIR_PATH=maestro/financial-connections
TEST_RESULTS_PATH=/tmp/test_results

# Create test results folder.
mkdir -p $TEST_RESULTS_PATH

# install APK.
adb install $BITRISE_APK_PATH

# Clear and start collecting logs
RETRY_COUNT=0
TEST_DIR_PATH=maestro/financial-connections

for TEST_FILE_PATH in "$TEST_DIR_PATH"/*.yaml; do
   # Check if tags are present in the test file
    if awk '/tags:/,/---/' "$TEST_FILE_PATH" | grep -q "$MAESTRO_TAGS"; then
        # Execute Maestro test flow and retries if failed.
        while [ "$RETRY_COUNT" -lt "$MAX_RETRIES" ]; do
            maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output $TEST_FILE_PATH.xml "$TEST_FILE_PATH" && break
            let RETRY_COUNT=RETRY_COUNT+1
            echo "Maestro test attempt $RETRY_COUNT failed. Retrying..."
            sleep 3
        done
        RETRY_COUNT=0
        if [ "$RETRY_COUNT" -eq "$MAX_RETRIES" ]; then
            echo "Maestro tests failed after $MAX_RETRIES attempts."
            exit 1
        fi
    fi
done

jrm $TEST_RESULTS_PATH/maestroReport.xml "$TEST_RESULTS_PATH/*.xml"