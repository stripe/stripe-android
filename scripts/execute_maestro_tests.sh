#!/usr/bin/env bash
set -e
set -o pipefail
set -x

# Install Maestro
curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:installDebug

# Run tests with retry.
retry=1
while [ $retry -le 3 ]; do
  maestro test maestro/financial-connections/
  if [ $? -eq 0 ]; then
    break
  fi
  retry=$((retry+1))
done