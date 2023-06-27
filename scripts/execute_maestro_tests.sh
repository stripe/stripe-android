#!/usr/bin/env bash
set -o pipefail
set -x

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

# Install Maestro
export MAESTRO_VERSION=1.29.0; curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Create test results folder.
mkdir -p /tmp/test_results

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:$task

# Clear and start collecting logs
maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output maestroReport.xml maestro/financial-connections/
