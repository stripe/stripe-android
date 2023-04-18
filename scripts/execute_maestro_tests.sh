#!/usr/bin/env bash
set -e
set -o pipefail
set -x

now=$(date +%F_%H-%M-%S)
echo $now

# Install Maestro
export MAESTRO_VERSION=1.21.3; curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:installDebug

# Start screen record (adb screenrecord has a 3 min limit).
adb shell "screenrecord /sdcard/$now-1.mp4" &

# Store the process ID
childpid=$!

echo "Executing maestro tests"
maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output maestroReport.xml maestro/financial-connections/; result=$? || true

echo "Waiting for record to finish"
kill -2 "$childpid"
wait "$childpid"

# Sleep for a short duration to allow the process to finalize the video file
sleep 3

echo "Pulling video results from device."
mkdir -p /tmp/test_results
cd /tmp/test_results
adb pull "/sdcard/$now-1.mp4" || true

exit "$result"
