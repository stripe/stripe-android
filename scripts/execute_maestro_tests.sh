#!/usr/bin/env bash
set -o pipefail
set -e

# Maestro tags to be executed. see https://maestro.mobile.dev/cli/tags
MAESTRO_TAGS=""
# Module to test
MODULE=""

while getopts ":t:m:" opt; do
  case $opt in
    t) MAESTRO_TAGS="$OPTARG"
    ;;
    m) MODULE="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    exit 1
    ;;
  esac
done

# Check if tags is empty
if [ -z "$MAESTRO_TAGS" ]
then
  echo "Tags parameter -t is required."
  exit 1
fi

# Check if module is empty
if [ -z "$MODULE" ]
then
  echo "Module parameter -m is required."
  exit 1
fi

# Set TEST_APP_ID based on MODULE
if [ "$MODULE" == "financial-connections" ]; then
  TEST_APP_ID="com.stripe.android.financialconnections.example"
elif [ "$MODULE" == "connect" ]; then
  TEST_APP_ID="com.stripe.android.connect.example"
else
  echo "Unknown module: $MODULE"
  exit 1
fi

echo "test_environment: $test_environment"
echo "MAESTRO_TAGS: $MAESTRO_TAGS"
echo "MODULE: $MODULE"

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

SCRIPT_DIR=$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)
TEST_DIR_PATH=maestro/$MODULE
TEST_RESULTS_PATH=/tmp/test_results
RETRY_COUNT=0

# Create test results folder.
mkdir -p $TEST_RESULTS_PATH

for TEST_FILE_PATH in "$TEST_DIR_PATH"/*.yaml; do
  # Skip glob when no matches
  [ -e "$TEST_FILE_PATH" ] || continue
  # Check if MAESTRO_TAGS are present in the test file
  if contains_tag "$TEST_FILE_PATH" "$MAESTRO_TAGS"; then
    # Just run the test if it's tagged as edge, if on an edge environment
    if [ "$test_environment" != "edge" ] || contains_tag "$TEST_FILE_PATH" "edge"; then
      TEST_NAME=$(basename "$TEST_FILE_PATH" .yaml)
      if python3 "${SCRIPT_DIR}/is_maestro_flow_quarantined.py" "$TEST_NAME"; then
        echo "Skipping quarantined Maestro flow: $TEST_NAME"
        continue
      fi
      ATTEMPT=0
      # Execute Maestro test flow and retry if failed (each try writes a distinct JUnit file).
      while [ "$ATTEMPT" -lt "$MAX_RETRIES" ]; do
        ATTEMPT=$((ATTEMPT + 1))
        OUT_XML="$TEST_RESULTS_PATH/${TEST_NAME}-try${ATTEMPT}.xml"
        if maestro test -e APP_ID=$TEST_APP_ID --format junit --output "$OUT_XML" "$TEST_FILE_PATH"; then
          break
        fi
        echo "Maestro test attempt $ATTEMPT failed. Retrying..."
        if [ "$ATTEMPT" -eq "$MAX_RETRIES" ]; then
          echo "Maestro tests failed after $MAX_RETRIES attempts."
          exit 1
        fi
      done
    else
      echo "Skipping test file without 'edge' tag: $TEST_FILE_PATH"
    fi
  fi
done
