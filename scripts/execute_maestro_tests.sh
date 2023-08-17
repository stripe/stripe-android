#!/usr/bin/env bash
set -o pipefail
set -x

# Retry mechanism for Maestro installation
MAX_RETRIES=3
RETRY_COUNT=0

# Get the first command line argument as the parameter
buildType=$1

if [ "$buildType" == "debug" ]; then
  task="installDebug"
elif [ "$buildType" == "release" ]; then
  task="installRelease"
else
  echo "Invalid parameter. Please use 'debug' or 'release'."
  exit 1
fi

now=$(date +%F_%H-%M-%S)
echo $now

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    export MAESTRO_VERSION=1.30.4
    curl -Ls "https://get.maestro.mobile.dev" | bash

    if [ $? -eq 0 ]; then
        # If successful, set PATH and break loop
        export PATH="$PATH":"$HOME/.maestro/bin"
        maestro -v
        break
    else
        let RETRY_COUNT=RETRY_COUNT+1
        echo "Attempt $RETRY_COUNT failed. Retrying..."
        sleep 5
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "Failed to download and install Maestro after $MAX_RETRIES attempts."
    exit 1
fi

# Create test results folder.
mkdir -p /tmp/test_results

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:$task

# Clear and start collecting logs
maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output maestroReport.xml maestro/financial-connections/
