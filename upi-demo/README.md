# UPI app-picker device demo

This module presents the real PaymentSheet with a fake **UPI Demo** payment method. It exercises the UPI PR's next-action parser, dedicated UPI app-chooser contract/activity, activity-result handling, and existing post-return PaymentIntent polling. The host app and all payment data are local simulations. No real UPI or Stripe backend is required.

- Stacked on the [UPI PaymentSheet implementation PR](https://github.com/stripe/stripe-android/pull/14826), branch `tyler/upi-intent-paymentsheet`.
- Receiver app project: [`banks/`](banks/README.md), included in this repository.
- Receiver scheme: `stripe-upi-demo://pay` (never the real `upi://` scheme).
- Checkout package: `com.stripe.android.upidemo`.
- Receiver packages: `com.stripe.upidemo.bank.blue`, `.green`, and `.orange`.

The receiver apps are a standalone Gradle project with three build flavors, so all three can be installed at the same time. This draft demo is a child of the implementation branch; it does not update the parent PR.

## Build and install

Connect an Android device (API 23+) with USB debugging enabled. Use the same JDK/Android SDK setup as stripe-android, including Android SDK 36. From the repository root, run:

```sh
./upi-demo/run-demo.sh DEVICE_SERIAL
```

Omit `DEVICE_SERIAL` when exactly one device/emulator is connected. The script builds and installs checkout and all three receiver APKs, then opens **UPI Demo Checkout**. It does not run tests or uninstall apps. It obtains the Android SDK from the checkout's `local.properties`, then `ANDROID_HOME` or `ANDROID_SDK_ROOT`. It looks for adb in SDK platform-tools, then `PATH`. `UPI_DEMO_ADB` and `UPI_DEMO_BANKS_DIR` override the adb executable and receiver-project paths. No machine-specific paths, SDK settings, or APKs are committed.

Alternatively, open this checkout in Android Studio and run the `upi-demo` debug configuration. Open `upi-demo/banks` separately and run `blueDebug`, `greenDebug`, and `orangeDebug`, or build them using its `./gradlew`. Its wrapper automatically finds the containing SDK checkout; `UPI_DEMO_SDK_WORKTREE` can point to another checkout.

## Device scenarios

Open checkout, tap **Start fake UPI payment**, and pay with **UPI Demo**. Android should offer the three demo banks. Select one and use its controls:

| Receiver action | Fake backend | Expected SDK behavior |
| --- | --- | --- |
| Approve and return | `processing`, then `succeeded` after 3 seconds | Standard PaymentSheet loading, followed by completion |
| Decline and return | `requires_payment_method` | Payment failure; the sheet may display the error until dismissed |
| Cancel / return no result | Still `requires_action` | Verify rather than trust cancellation; stop after the 15-second polling window |
| Return SUCCESS without approving | Still `requires_action` | Must not report payment success; verification eventually stops |
| Approve, stay here | Settles after 3 seconds | Use Android Back to return without a success result; SDK verifies the approved payment |

Also try dismissing the chooser, rotating the device, and backgrounding/restoring checkout while a bank app is open. The checkout log shows simulated API requests and the final PaymentSheet result. It is retained across process recreation. You can also watch:

```sh
adb -s DEVICE_SERIAL logcat -s UpiDemo
```

For the zero-app case, uninstall the three demo banks in Android Settings and start another fake payment. The SDK should show the no-compatible-app error without a QR/browser fallback. Install only one receiver to inspect the single-handler case, then reinstall all three to compare. Real bank apps do not match the fake scheme.

## How the simulation works

1. **Start fake UPI payment** creates a fresh fake PaymentIntent in app-local preferences.
2. The demo application's `ConnectionFactory` hook supplies fake Elements Session, confirmation, and retrieval responses to the real SDK. SDK requests in this dedicated app never fall through to Stripe; unsupported requests return a mock error.
3. Confirm returns the normal UPI next action with a `stripe-upi-demo://pay` URL. PaymentSheet's `UpiNextActionHandler` launches `UpiAppChooserContract` and `UpiAppChooserActivity`; the dedicated `UpiAppChooserViewModel` permits the fake scheme only in debug builds on this demo branch. No browser contract, browser starter, or browser launcher is involved. The demo app declares package visibility for both the fake scheme and the `pay` host so Android can match the receivers' intent filters.
4. The selected receiver updates a fake ContentProvider in checkout to simulate a backend decision, independently of any `response` extra returned through Android. The provider only accepts the current fake payment ID and the `approve`/`decline` actions.
5. **Approve and return** stores `processing` with a settlement timestamp three seconds in the future, then calls `setResult(RESULT_OK, Intent(...))` with a sample `response` extra and `finish()`. This closes the bank activity and delivers an activity result to the SDK; it does not open a callback URL. A subsequent status read after the deadline promotes the fake payment to `succeeded`.
6. `UpiAppChooserActivity` returns an unknown-outcome `PaymentFlowResult` to PaymentSheet, whose existing result processor polls the fake PaymentIntent responses, including `processing`, for up to 15 seconds. It does not use `PollingActivity` and does not trust the bank app's `response` extra. The activity also supports manual return without a useful result and saves launch state to avoid reopening the chooser after recreation. The fake backend state and logs survive process recreation in SharedPreferences; that is not a guarantee that every interrupted UI flow recovers.

### Package visibility fix found by the demo

The original scheme-only package query did not expose the receivers on the tested Android 36 emulator because their intent filters also require host `pay`. Matching both the scheme and host made all three apps visible to the SDK's handler lookup. The parent UPI implementation now includes the real `upi://pay` query in `paymentsheet/src/main/AndroidManifest.xml`. This demo adds the fake-scheme query in its own manifest; neither query needs browser infrastructure.

### Limits and safety

The exported provider is deliberately a local simulation, not a secure server or real payment verification mechanism. This demonstrates Android chooser/return behavior and SDK orchestration, not interoperability with actual UPI banking apps.

The fake API uses a dummy publishable key, advertises only UPI, and has no real Stripe network fallback. SDK analytics are disabled in the demo application. The bank apps have no internet permission. Do not use this provider or fake network hook in production, and do not fulfill orders based on client-reported success. Real integrations still require Stripe/backend verification and a server-supplied `mobile_auth_url`; this demo does not make that API field available.

### Validation

After the refactor: checkout and all three bank debug APKs build successfully; focused UPI debug tests (41 total, one expected release-only skip) and release ViewModel tests (18 total, one expected debug-only skip) pass. PaymentSheet Detekt and both PaymentSheet/payments-core API checks pass. No device install or payment walkthrough was run for this update.

Before the dedicated-contract refactor, verified on an Android 36 emulator: the run script builds and installs all four apps; checkout shows all three banks in the native chooser; approving in Blue returns to SDK polling (`processing` → `succeeded`) and PaymentSheet reports `Completed`. Observed four `processing` retrievals followed by a fifth returning `succeeded`. That device flow has not been rerun after the refactor. The new chooser has focused unit/lifecycle coverage, including debug acceptance and release rejection of the fake scheme. Other scenarios above remain manual checks. Real UPI apps, real Stripe requests, and a physical device have not been validated by this demo.
