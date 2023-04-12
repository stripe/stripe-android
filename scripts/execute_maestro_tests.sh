#!/usr/bin/env bash

now=$(date +%F_%H-%M-%S)
echo $now

# Install Maestro
export MAESTRO_VERSION=1.21.3; curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Compile and install APK.
#./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:installDebug

# Start screen record (adb screenrecord has a 3 min limit, for now take consequent recordings.
adb shell "screenrecord /sdcard/$now-1.mp4; screenrecord /sdcard/$now-2.mp4; screenrecord /sdcard/$now-3.mp4; screenrecord /sdcard/$now-4.mp4" &
childpid=$!

# Clear and start collecting logs
maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output maestroReport.xml maestro/financial-connections/
result=$?

# Stop video recording and pull record parts.
kill "$childpid"
wait "$childpid"
adb pull "/sdcard/$now-1.mp4" || true
adb pull "/sdcard/$now-2.mp4" || true
adb pull "/sdcard/$now-3.mp4" || true
adb pull "/sdcard/$now-4.mp4" || true

# Merge the pulled mp4 files
echo "file '$now-1.mp4'" > concat-list.txt
[ -f "$now-2.mp4" ] && echo "file '$now-2.mp4'" >> concat-list.txt
[ -f "$now-3.mp4" ] && echo "file '$now-3.mp4'" >> concat-list.txt
[ -f "$now-4.mp4" ] && echo "file '$now-4.mp4'" >> concat-list.txt

ffmpeg -f concat -safe 0 -i concat-list.txt -c copy merged-$now.mp4
rm concat-list.txt

exit "$result"