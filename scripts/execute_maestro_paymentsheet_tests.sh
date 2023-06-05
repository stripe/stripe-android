#!/usr/bin/env bash
set -o pipefail
set -x

now=$(date +%F_%H-%M-%S)
echo $now

# Install Maestro
export MAESTRO_VERSION=1.21.3; curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Compile and install APK.
./gradlew :paymentsheet-example:installDebug

# Start screen record (adb screenrecord has a 3 min limit). Can append more recordings as needed.
adb shell "screenrecord /sdcard/$now-1.mp4; screenrecord /sdcard/$now-2.mp4" &

# Store the process ID
childpid=$!

# Clear and start collecting logs
maestro test --format junit --output maestroReport.xml maestro/paymentsheet
result=$?

# Wait for the recording process to finish
kill -2 "$childpid"
wait "$childpid"

# Sleep for a short duration to allow the process to finalize the video file
sleep 3

# Pull the video file from the device
mkdir -p /tmp/test_results
cd /tmp/test_results
adb logcat -d > /tmp/test_results/log.txt
adb pull "/sdcard/$now-1.mp4" || true
adb pull "/sdcard/$now-2.mp4" || true

exit "$result"
