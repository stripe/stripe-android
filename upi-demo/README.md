# Local UPI app-picker demo

This module presents the real PaymentSheet with a fake **UPI Demo** payment method. It exercises the UPI PR's next-action parser, native chooser, redirect launcher, activity-result handling, and post-redirect PaymentIntent polling. The host app and all payment data are local simulations. No real UPI or Stripe backend is required.

- SDK branch: `tyler/upi-intent-device-demo`, based on `tyler/upi-intent-paymentsheet` at `5b5a837734`.
- SDK worktree: `/private/tmp/stripe-android-upi-demo`.
- Receiver app project: `/Users/tjclawson/stripe/upi-demo-banks`.
- Receiver scheme: `stripe-upi-demo://pay` (never the real `upi://` scheme).
- Checkout package: `com.stripe.android.upidemo`.
- Receiver packages: `com.stripe.upidemo.bank.blue`, `.green`, and `.orange`.

This branch is local only. It does not modify the existing UPI PR. The receiver apps are a separate local project with three build flavors, so all three can be installed at the same time.

## Build and install

Connect a device with USB debugging enabled. Use the same JDK/Android SDK setup as stripe-android. Then run:

```sh
cd /private/tmp/stripe-android-upi-demo
./upi-demo/run-demo.sh DEVICE_SERIAL
```

Omit `DEVICE_SERIAL` when exactly one device/emulator is connected. The script builds and installs checkout and all three receiver APKs, then opens **UPI Demo Checkout**. It does not run tests or uninstall apps. `UPI_DEMO_ADB` and `UPI_DEMO_BANKS_DIR` can override the default adb and receiver-project paths.

Alternatively, open this worktree in Android Studio and run the `upi-demo` debug configuration. Open the receiver project separately and run `blueDebug`, `greenDebug`, and `orangeDebug`, or build them using its `./gradlew`. Its wrapper delegates to this SDK worktree; set `UPI_DEMO_SDK_WORKTREE` if you move the SDK worktree.

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
3. Confirm returns the normal UPI next action with a `stripe-upi-demo://pay` URL. This local branch permits that scheme only in debug SDK builds. The demo app declares package visibility for that scheme.
4. The selected receiver updates a fake ContentProvider in checkout to simulate a backend decision, independently of any `response` extra returned through Android. The provider only accepts the current fake payment ID and the `approve`/`decline` actions.
5. The existing SDK result processor polls those fake retrieval responses. The status persists if Android recreates the checkout process.

The exported provider is deliberately a local simulation, not a secure server or real payment verification mechanism. This demonstrates Android chooser/return behavior and SDK orchestration, not interoperability with actual UPI banking apps.

No build, install, or test was run for this local demo change, as requested.
