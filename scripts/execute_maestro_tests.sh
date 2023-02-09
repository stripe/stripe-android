#!/usr/bin/env bash

# Uninstall existing app on cached AVD.
adb uninstall com.stripe.android.financialconnections.example || true

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:installDebug

# Run tests.
for file in "$folder"/*; do
  if [ -f "$file" ]; then
    echo "maestro record maestro/financial-connections/$file"
  fi
done
