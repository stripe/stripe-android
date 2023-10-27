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
MAX_RETRIES=3
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

# Create test results folder.
mkdir -p /tmp/test_results

# install APK.
adb install $BITRISE_APK_PATH

# Clear and start collecting logs
maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --include-tags=$MAESTRO_TAGS --output maestroReport.xml maestro/financial-connections