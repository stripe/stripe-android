# Stripe Android SDK payment card scan module
This module provides support for the standalone Stripe CardScan product.

# Overview
This library provides a user interface through which users can scan payment cards and extract information from them.

Note that this is a standalone SDK and, while compatible with, does not directly integrate with the [PaymentIntent](https://stripe.com/docs/api/payment_intents) API nor with [next_action](https://stripe.com/docs/api/errors#errors-payment_intent-next_action).

This library can be used entirely outside of a Stripe integration and with other payment processing providers.

# Requirements
- Android API level 21 or higher
- AndroidX compatibility
- Kotlin coroutine compatibility

Note: Your app does not have to be written in kotlin to integrate this library, but must be able to depend on kotlin functionality.

# Example
See the `stripecardscan-example` directory for an example application that you can try for yourself!

# Integration
* In app/build.gradle, add these dependencies:
    ```gradle
    dependencies {
        implementation 'com.stripe:stripecardscan:$stripeVersion'
    }
    ```

## Use TFLite in Google play to reduce binary size

CardScan Android SDK uses a portable TFLite runtime to execute machine learning models, if your application is released through Google play, you could instead use the Google play runtime, this would reduce the SDK size by ~400kb.

To do so, configure your app's dependency on stripecardscan as follows.
```
    implementation('com.stripe:stripecardscan:$stripeVersion') {
      exclude group: 'com.stripe', module: 'ml-core-cardscan' // exclude the cardscan-specific portable tflite runtime
    }
    implementation('com.stripe:ml-core-googleplay:$stripeVersion') // include the google play tflite runtime
```

# Credit Card OCR

Add `CardScanSheet` in your activity or fragment where you want to invoke the verification flow

Note: the `create` method must be called in your fragment or activity’s `onCreate` method in order to register the `ActivityResultListener` with your activity.

a. Initialize `CardScanSheet`
b. When it's time to invoke the verification flow, display the sheet with `CardScanSheet.present()`
c. When the verification flow is finished, the sheet will be dismissed and the `onFinished` block will be called with a `CardScanSheetResult`

```kotlin
class LaunchActivity : AppCompatActivity {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launch)

    /**
     * Create a [CardScanSheet] instance with [ComponentActivity].
     *
     * This API registers an [ActivityResultLauncher] into the
     * [ComponentActivity], it must be called before the [ComponentActivity]
     * is created (in the onCreate method).
     */
    val cardScanSheet = CardScanSheet.create(
      from = LaunchActivity.this,
    )

    findViewById(R.id.scanCardButton).setOnClickListener { _ ->
      cardScanSheet.present(
        from = LaunchActivity.this,
      ) { cardScanSheetResult ->
        when (cardScanSheetResult) {
          is CardScanSheet.Completed -> {
            /*
             * The user scanned a card. The result of the scan can be found
             * by querying the stripe card image verification endpoint with
             * the CVI_ID, CVI_SECRET, and Stripe secret key.
             * 
             * Details about the card itself are returned in the `scannedCard`
             * field of the result.
             */
            Log.i(cardScanSheetResult.scannedCard.pan)
          }
          is CardScanSheet.Canceled -> {
            /*
             * The scan was canceled. This could be because any of the
             * following reasons (returned as the
             * [CancellationReason] in the result):
             *
             * - Closed - the user pressed the X
             * - Back - the user pressed the back button
             * - UserCannotScan - the user is unable to scan this card
             * - CameraPermissionDenied - the user did not grant permissions
             */
            Log.i(cardScanSheetResult.reason)
          }
          is CardScanSheet.Failed -> {
            /*
             * The scan failed. The error that caused the failure is
             * included in the [Throwable] `error` field of the verification
             * result.
             */
            Log.e(cardScanSheetResult.error.message)
          }
        }
      }
    }
  }
}
```
