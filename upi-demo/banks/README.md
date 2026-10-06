# UPI demo bank apps

Three separately installable Android apps from one shared Java implementation:

| Flavor | App label | Application ID |
| --- | --- | --- |
| blue | Demo Bank Blue | `com.stripe.upidemo.bank.blue` |
| green | Demo Bank Green | `com.stripe.upidemo.bank.green` |
| orange | Demo Bank Orange | `com.stripe.upidemo.bank.orange` |

All register `ACTION_VIEW`, `DEFAULT`, and `BROWSABLE` for `stripe-upi-demo://pay`. None handles the real `upi://` scheme. These apps have no network permission and never move money.

This is a standalone Gradle project, not a published SDK module. Its `gradlew` forwards to the containing stripe-android checkout's wrapper and reuses its Android SDK location (`local.properties`, `ANDROID_HOME`, or `ANDROID_SDK_ROOT`). Set `UPI_DEMO_SDK_WORKTREE` to use a different SDK checkout. It requires Android SDK 36 and uses AGP 8.13.2 and Java 11 source compatibility.

From the stripe-android repository root, build the receivers:

```sh
./upi-demo/banks/gradlew :app:assembleBlueDebug :app:assembleGreenDebug :app:assembleOrangeDebug
```

Or build and install checkout plus all three receivers:

```sh
./upi-demo/run-demo.sh DEVICE_SERIAL
```

Omit `DEVICE_SERIAL` if exactly one device is connected. The script installs only the four demo packages, then opens checkout; it does not uninstall anything. You can also open this directory separately in Android Studio and select `blueDebug`, `greenDebug`, or `orangeDebug`. Configure its Android SDK normally if opening it independently of the wrapper.

**Approve and return** first updates checkout's exported demo ContentProvider, setting the fake PaymentIntent to `processing` and scheduling simulated success three seconds later. It then returns an Android `response` extra with `Status=SUCCESS` using `setResult(RESULT_OK, ...)` and calls `finish()`. Android returns to PaymentSheet's dedicated `UpiAppChooserActivity` without needing a return URL or browser. PaymentSheet ignores the status string and retrieves the fake API state independently.

This provider is deliberately insecure local demo plumbing, not a payment verification mechanism for real apps. See the [checkout README](../README.md) for every button, expected outcomes, validation, and limitations.
