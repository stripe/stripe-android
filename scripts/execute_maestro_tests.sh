#!/usr/bin/env bash
set -e
set -o pipefail
set -x

# Install Maestro
export MAESTRO_VERSION=1.21.3; curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:installDebug

# Run tests with retry.
retry=1
while [ $retry -le 3 ]; do
  echo "Running Maestro tests. Attempt $retry"
  maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output maestroReport.xml maestro/financial-connections
  if [ $? -eq 0 ]; then
    break
  fi
  retry=$((retry+1))
done