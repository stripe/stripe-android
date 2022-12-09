#!/usr/bin/env bash


# Uninstall existing app on cached AVD.
adb uninstall com.stripe.android.financialconnections.example || true

# Create folder for reports
mkdir -p financial-connections-example/build/reports

# create log file and allow writes.
rm financial-connections-example/build/reports/emulator.log
touch financial-connections-example/build/reports/emulator.log
chmod 777 financial-connections-example/build/reports/emulator.log

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:installDebug

# Clear logs
adb logcat -c
adb logcat >> financial-connections-example/build/reports/emulator.log &
if maestro test --format junit maestro/financial-connections/testmode-linkmore.yaml; then
  echo "Maestro tests succeeded" >&2
  exit 0
else
  echo "Maestro tests failed" >&2
  exit 1
fi

# Pull recording from Device sdcard. (uncomment when `maestro record` gets fixed).
# cd financial-connections-example/build/reports/ && adb pull /sdcard/maestro-screenrecording.mp4 || true
