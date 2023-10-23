#!/usr/bin/env bash
set -o pipefail
set -x
set -e

# Define maestro tests path from command line arg
MAESTRO_PATH=$1

export MAESTRO_VERSION=1.31.0

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
maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output maestroReport.xml $MAESTRO_PATH