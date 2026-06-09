# Running PaymentSheet E2E Tests

Run the same Gradle Managed Device suite used in CI with:

```bash
./gradlew \
  :paymentsheet-example:pixel2api33BrowserBaseDebugAndroidTest \
  --init-script build-configuration/instrumentation-test-init.gradle
```

CI runs the same task with retries and sharding:

```bash
./scripts/retry.sh 3 ./gradlew \
  -Pandroid.experimental.androidTest.numManagedDeviceShards=3 \
  -Pandroid.experimental.testOptions.managedDevices.maxConcurrentDevices=3 \
  :paymentsheet-example:pixel2api33BrowserBaseDebugAndroidTest \
  -PSTRIPE_PAYMENTSHEET_EXAMPLE_SENTRY_DSN=$STRIPE_PAYMENTSHEET_EXAMPLE_SENTRY_DSN \
  --init-script build-configuration/instrumentation-test-init.gradle
```

The `pixel2api33Browser` managed device uses the Google API system image so browser-based
authorization flows run against a Chrome-capable emulator. Local runs can still skip
browser-specific tests if the required browser is unavailable.
