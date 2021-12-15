# Stripe Android SDK payment card scan module
This module provides support for the standalone Stripe CardScan product.

# Overview
This library provides a user interface through which users can scan payment cards and extract information from them. It uses the Stripe Publishable Key to authenticate with Stripe services.

Note that this is a standalone SDK and, while compatible with, does not directly integrate with the [PaymentIntent](https://stripe.com/docs/api/payment_intents) API nor with [next_action](https://stripe.com/docs/api/errors#errors-payment_intent-next_action).

This library can be used entirely outside of a Stripe integration and with other payment processing providers.

# Requirements
- Android API level 21 or higher
- AndroidX compatibility
- Kotlin coroutine compatibility

Note: Your app does not have to be written in kotlin to integrate this library, but must be able to depend on kotlin functionality.

# Integration
* In app/build.gradle, add these dependencies:
    ```gradle
    dependencies {
        implementation 'com.stripe:stripecardscan:$stripeVersion'
    }
    ```

# Usage
1. Create a `CardImageVerificationIntent` (CIV Intent) on your backend

    Perform the following tasks on your server:
    a. Create a CIV intent using the following:
        i. your Stripe `secret key`  
        ii. the `bin`  of the card to verify
        iii. the `last4` of the card to verify 

    b. Provide the CIV intent's `id` and the `client_secret` to the SDK

2. Add `CardVerificationSheet` in your activity or fragment where you want to invoke the verification flow

    Note: the `create` method must be called in your fragment or activity’s `onCreate` method in order to register the `ActivityResultListener` with your activity.

    a. Initialize `CardVerificationSheet`  with your `publishableKey` and the `id, client secret`
    b. When it's time to invoke the verification flow, display the sheet with `CardVerificationSheet.present()`
    c. When the verification flow is finished, the sheet will be dismissed and the `onFinished` block will be called with a `CardVerificationSheetResult`

    ```kotlin
    class LaunchActivity : AppCompatActivity {
    
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
    
        /**
         * Create a [CardVerificationSheet] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the
         * [ComponentActivity], it must be called before the [ComponentActivity]
         * is created (in the onCreate method).
         *
         * see https://github.com/stripe/stripe-android/blob/3e92b79190834dc3aab1c2d9ac2dfb7bc343afd2/payments-core/src/main/java/com/stripe/android/payments/paymentlauncher/PaymentLauncher.kt#L52
         */
        val cardVerificationSheet = CardVerificationSheet.create(
          from = LaunchActivity.this,
          stripePublishableKey = "stripe_key",
        )
    
        findViewById(R.id.scanCardButton).setOnClickListener { _ ->
          cardVerificationSheet.present(
            from = LaunchActivity.this,
            cardImageVerificationIntentId = "civ_id",
            cardImageVerificationIntentSecret = "civ_client_secret",
          ) { cardVerificationSheetResult ->
            when (cardVerificationSheetResult) {
              is CardVerificationSheetResult.Completed -> {
                /*
                 * The user scanned a card. The result of the scan can be found
                 * by querying the stripe card image verification endpoint with
                 * the CVI_ID, CVI_SECRET, and Stripe secret key.
                 * 
                 * Details about the card itself are returned in the `scannedCard`
                 * field of the result.
                 */
                Log.i(cardVerificationSheetResult.scannedCard.pan)
              }
              is CardVerificationSheetResult.Canceled -> {
                /*
                 * The scan was canceled. This could be because any of the
                 * following reasons (returned as the
                 * [CardVerificationSheetCancelationReason] in the result):
                 *
                 * - Closed - the user pressed the X
                 * - Back - the user pressed the back button
                 * - UserCannotScan - the user is unable to scan this card
                 * - CameraPermissionDenied - the user did not grant permissions
                 */
                Log.i(cardVerificationSheetResult.reason)
              }
              is CardVerificationSheetResult.Failed -> {
                /*
                 * The scan failed. The error that caused the failure is
                 * included in the [Throwable] `error` field of the verification
                 * result.
                 */
                Log.e(cardVerificationSheetResult.error.message)
              }
            }
          }
        }
      }
    }
   ```

# Example
See the stripecardscan-example directory for an example application that you can try for yourself!
