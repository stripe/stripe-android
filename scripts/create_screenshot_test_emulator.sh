if [ ! -d "$ANDROID_HOME" ] ; then
  echo "Cannot find \$ANDROID_HOME."
  exit
fi

if [ ! -d "$ANDROID_HOME/cmdline-tools" ]; then
  echo "Cannot find Android SDK Command-line Tools. Install them via SDK Manager > SDK Tools."
  exit
fi

if [[ $(uname -m) == 'arm64' ]]; then
  ARCHITECTURE='arm64-v8a'
else
  ARCHITECTURE=$(uname -m)
fi
echo "Creating emulator with architecture: $ARCHITECTURE"

"$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" "--verbose" "create" "avd" "--force" "--name" "test" "--device" "Nexus 6" "--package" "system-images;android-28;google_apis;$ARCHITECTURE" "--tag" "google_apis" "--abi" "$ARCHITECTURE" "--sdcard" "512M"


if [ ! -d "$ANDROID_HOME" ] ; then
  echo "Cannot find \$ANDROID_HOME."
  exit
fi

"$ANDROID_HOME/emulator/emulator" "@test" "-verbose" "-netdelay" "none" "-gpu" "swiftshader_indirect"
