#!/usr/bin/env bash
set -o pipefail
set -x

now=$(date +%F_%H-%M-%S)
echo $now

# Install Maestro
export MAESTRO_VERSION=1.30.4; curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Create test results folder.
mkdir -p /tmp/test_results

# Compile and install APK.
./gradlew :paymentsheet-example:installDebug

# Clear and start collecting logs
maestro test --format junit --output maestroReport.xml maestro/customersheet
