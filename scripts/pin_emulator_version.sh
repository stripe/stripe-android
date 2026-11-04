#!/bin/bash
set -eo pipefail

# Replaces the "emulator" package already present in $ANDROID_HOME with a known-good,
# explicitly pinned build, regardless of whatever version the CI stack's image bundles.
#
# This is CI's own pin, separate from (and in addition to) the `emulator_build_number`
# input on the `avd-manager@2` Bitrise step: Gradle Managed Device tests
# (e.g. pixel2api33DebugAndroidTest) boot the emulator straight from $ANDROID_HOME/emulator
# without going through avd-manager, so they need this binary swapped in directly.

EMULATOR_BUILD_NUMBER=${EMULATOR_BUILD_NUMBER:-16428233}
EMULATOR_VERSION=${EMULATOR_VERSION:-37.2.12} # must match the build number above

if [ -z "$ANDROID_HOME" ]; then
  echo "ANDROID_HOME is not set"
  exit 1
fi

OS=$(uname -s)
if [ "$OS" = "Darwin" ]; then
  ARCH=$(uname -m)
  if [ "$ARCH" = "arm64" ]; then
    PLATFORM="darwin_aarch64"
  else
    PLATFORM="darwin_x64"
  fi
else
  PLATFORM="linux_x64"
fi

DOWNLOAD_URL="https://dl.google.com/android/repository/emulator-${PLATFORM}-${EMULATOR_BUILD_NUMBER}.zip"
ZIP_PATH="/tmp/emulator-${EMULATOR_BUILD_NUMBER}.zip"
EXTRACT_DIR="/tmp/emulator-${EMULATOR_BUILD_NUMBER}"

echo "Pinning Android emulator to build ${EMULATOR_BUILD_NUMBER}..."
curl -fSL --connect-timeout 30 --max-time 600 "$DOWNLOAD_URL" -o "$ZIP_PATH"

rm -rf "$EXTRACT_DIR"
mkdir -p "$EXTRACT_DIR"
unzip -q "$ZIP_PATH" -d "$EXTRACT_DIR"
rm "$ZIP_PATH"

rm -rf "$ANDROID_HOME/emulator"
mv "$EXTRACT_DIR/emulator" "$ANDROID_HOME/emulator"
rmdir "$EXTRACT_DIR"

# The SDK manager (and the Gradle Managed Device plugin, which calls into the same
# repository-scanning code) only recognizes a directory as containing an up-to-date
# "emulator" package if it finds this package.xml with a matching revision. Without
# it, a manually-dropped-in binary looks unmanaged, so the GMD Setup task re-installs
# its own copy of the *latest* emulator alongside ours (into "emulator-2") and uses
# that instead, silently undoing this pin.
IFS='.' read -r EMULATOR_MAJOR EMULATOR_MINOR EMULATOR_MICRO <<< "$EMULATOR_VERSION"
cat > "$ANDROID_HOME/emulator/package.xml" <<EOF
<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02" xmlns:ns5="http://schemas.android.com/repository/android/generic/02"><license id="android-sdk-license" type="text">Android SDK License</license><localPackage path="emulator" obsolete="false"><type-details xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:genericDetailsType"/><revision><major>${EMULATOR_MAJOR}</major><minor>${EMULATOR_MINOR}</minor><micro>${EMULATOR_MICRO}</micro></revision><display-name>Android Emulator</display-name><uses-license ref="android-sdk-license"/></localPackage></ns2:repository>
EOF

echo "Pinned emulator version:"
"$ANDROID_HOME/emulator/emulator" -version
