#!/usr/bin/env bash

reports_path="financial-connections-example/build/reports"
logs_file_path="$reports_path/emulator.log"
now=$(date +%F_%H-%M-%S)
echo $now

# Uninstall existing app on cached AVD.
adb uninstall com.stripe.android.financialconnections.example || true

# Create folder for reports
mkdir -p $reports_path

# create log file and allow writes.
rm $logs_file_path
touch $logs_file_path
chmod 777 $logs_file_path

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:installDebug

# Start screen record (adb screenrecord has a 3 min limit, for now take consequent recordings.
adb shell "screenrecord /sdcard/$now-1.mp4; screenrecord /sdcard/$now-2.mp4; screenrecord /sdcard/$now-3.mp4; screenrecord /sdcard/$now-4.mp4" &
childpid=$!

# Clear and start collecting logs
adb logcat -c
adb logcat >> $logs_file_path &
maestro test --format junit maestro/financial-connections
result=$?

# Stop video recording and pull record parts.
kill "$childpid"
wait "$childpid"
cd $reports_path/
adb pull "/sdcard/$now-1.mp4" || true
adb pull "/sdcard/$now-2.mp4" || true
adb pull "/sdcard/$now-3.mp4" || true
adb pull "/sdcard/$now-4.mp4" || true

exit "$result"
