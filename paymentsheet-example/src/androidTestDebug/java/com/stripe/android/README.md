# PaymentSheet Screenshot Tests
# Lib: https://github.com/pedrovgs/Shot

To Run Screenshot tests, takes 30 seconds a test and is long running:
./gradlew executeScreenshotTests

To Run Screenshot tests for a specific file (and test method):
./gradlew executeScreenshotTests -Pandroid.testInstrumentationRunnerArguments.class=com.stripe.android.screenshot.TestPaymentSheetScreenshots#testPaymentSheetReturningCustomerLight

After running you can check the report here: ./stripe-android/paymentsheet-example/build/reports/shot/debug/verification/index.html

To Update the baseline Snapshots:
./gradlew executeScreenshotTests -Precord

After running you can check baseline result html in /Users/skyler/stripe/stripe-android/paymentsheet-example/build/reports/shot/debug/record/index.html
and you can check the screenshots here: ./stripe-android/paymentsheet-example/screenshots/debug/

## Running BrowserStack Tests

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
