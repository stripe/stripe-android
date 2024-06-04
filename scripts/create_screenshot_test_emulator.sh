# Script to generate and start an emulator that is similar to the one used in our ci
# Useful for locally figuring out failed tests and generating the record screenshots

if [ ! -d "$ANDROID_HOME" ] ; then
  echo "Cannot find \$ANDROID_HOME."
  exit
fi

if [ ! -d "$ANDROID_HOME/cmdline-tools" ]; then
  echo "Cannot find Android SDK Command-line Tools. Install them via SDK Manager > SDK Tools."
  exit
fi

echo "download system image, if not already downloaded"
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "--install" "system-images;android-29;google_apis;arm64-v8a"

echo "creating emulator"
"$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" "--verbose" "create" "avd" "--force" "--name" "screenshot_test_local" "--device" "Nexus 6" "--package" "system-images;android-29;google_apis;arm64-v8a" "--tag" "google_apis" "--abi" "arm64-v8a" "--sdcard" "512M"

echo "starting emulator"
"$ANDROID_HOME/emulator/emulator" "@screenshot_test_local" "-verbose" "-netdelay" "none" "-gpu" "swiftshader_indirect"
