#!/usr/bin/env bash
set -e
set -o pipefail
set -x

# Install Maestro
export MAESTRO_VERSION=1.21.3; curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Compile and install APK.
./gradlew :paymentsheet-example:installDebug

# Run tests with retry.
retry=1
while [ $retry -le 3 ]; do
  echo "Running Maestro tests. Attempt $retry"
  maestro test --format junit --output maestroReport.xml maestro/paymentsheet
  if [ $? -eq 0 ]; then
    break
  fi
  retry=$((retry+1))
done
