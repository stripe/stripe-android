# Running BrowserStack Tests

You can run BrowserStack tests with the following command:

```bash
./gradlew :paymentsheet-example:assembleDebugAndroidTest :paymentsheet-example:assembleDebug && \
  BROWSERSTACK_USERNAME=YOUR_BROWSERSTACK_USERNAME \
  BROWSERSTACK_ACCESS_KEY=YOUR_BROWSERSTACK_ACCESS_KEY \
  GOOGLE_PLAY_TESTING_EMAIL=YOUR_GOOGLE_PLAY_EMAIL \
  GOOGLE_PLAY_TESTING_SECRET=YOUR_GOOGLE_PLAY_PASSWORD \
  python scripts/browserstack.py \
  --test \
  --apk paymentsheet-example/build/outputs/apk/debug/paymentsheet-example-debug.apk \
  --espresso paymentsheet-example/build/outputs/apk/androidTest/debug/paymentsheet-example-debug-androidTest.apk
```

Get your `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` through [browserstack](https://app-automate.browserstack.com/dashboard/v2).
Set `GOOGLE_PLAY_TESTING_EMAIL` and `GOOGLE_PLAY_TESTING_SECRET` to the credentials of a Google account with [Test Card Suite](https://developers.google.com/pay/api/android/guides/resources/test-card-suite) access.
