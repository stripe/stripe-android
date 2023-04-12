#!/usr/bin/env bash
set -e
set -o pipefail
set -x

reports_path="financial-connections-example/build/reports"
now=$(date +%F_%H-%M-%S)
echo $now

# Create folder for reports
mkdir -p $reports_path

# Install Maestro
export MAESTRO_VERSION=1.21.3; curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH":"$HOME/.maestro/bin"
maestro -v

# Install FFmpeg
sudo apt-get update
sudo apt-get install -y ffmpeg

# Compile and install APK.
./gradlew \
-PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL \
:financial-connections-example:installDebug

# Start screen record (adb screenrecord has a 3 min limit, for now take consequent recordings.
adb shell "screenrecord /sdcard/$now-1.mp4; screenrecord /sdcard/$now-2.mp4; screenrecord /sdcard/$now-3.mp4; screenrecord /sdcard/$now-4.mp4" &
childpid=$!

# Execute Maestro tests
maestro test -e APP_ID=com.stripe.android.financialconnections.example --format junit --output maestroReport.xml maestro/financial-connections
result=$?

# Stop video recording and pull record parts.
kill "$childpid"
wait "$childpid"
cd $reports_path/
adb pull "/sdcard/$now-1.mp4" || true
adb pull "/sdcard/$now-2.mp4" || true
adb pull "/sdcard/$now-3.mp4" || true
adb pull "/sdcard/$now-4.mp4" || true

# Create a list of video files to concatenate
echo "file '$now-1.mp4'" > video_list.txt
echo "file '$now-2.mp4'" >> video_list.txt
echo "file '$now-3.mp4'" >> video_list.txt
echo "file '$now-4.mp4'" >> video_list.txt

# Use FFmpeg to concatenate the video files
output_file="${now}-combined.mp4"
ffmpeg -f concat -safe 0 -i video_list.txt -c copy $output_file

# Remove individual video files and video_list.txt
rm "$now-1.mp4" "$now-2.mp4" "$now-3.mp4" "$now-4.mp4" video_list.txt

exit "$result"