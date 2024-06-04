# Running BrowserStack Tests

You can run BrowserStack tests with the following command:

```bash
./gradlew :paymentsheet-example:assembleDebugAndroidTest :paymentsheet-example:assembleDebug && \
  BROWSERSTACK_USERNAME=YOUR_BROWSERSTACK_USERNAME \
  BROWSERSTACK_ACCESS_KEY=YOUR_BROWSERSTACK_ACCESS_KEY \
  python scripts/browserstack.py \
  --test \
  --apk paymentsheet-example/build/outputs/apk/debug/paymentsheet-example-debug.apk \
  --espresso paymentsheet-example/build/outputs/apk/androidTest/debug/paymentsheet-example-debug-androidTest.apk
```

Get your `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` through [browserstack](https://app-automate.browserstack.com/dashboard/v2).
