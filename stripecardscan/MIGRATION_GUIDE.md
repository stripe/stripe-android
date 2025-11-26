# Migrating from Stripe CardScan to Google Pay Card Recognition API

> This guide is intended for developers who used CardScanSheet directly in their apps.

## Request Google Pay API production access
Before you migrate, you must [request production access](https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access) to the Google Pay API using the [Google Pay & Wallet Console](https://pay.google.com/business/console?utm_source=devsite&utm_medium=devsite&utm_campaign=devsite).

## Update Dependencies

1. Remove the old Stripe CardScan dependency 
2. Add the necessary Google Play service dependency to your app-level `build.gradle` (or `build.gradle.kts`).

### Remove Stripe CardScan Dependency

Remove the line for the `stripecardscan` module:

```groovy
// REMOVE THIS LINE  
implementation("com.stripe:stripecardscan:VERSION")
```

### Add Google Pay Dependency

Add the Google Play service library according to the [Google documentation](https://developers.google.com/pay/api/android/guides/setup#app%20dependencies), and enable Google Pay API in `AndroidManifest.xml`:

```xml
<application>
    <meta-data
        android:name="com.google.android.gms.wallet.api.enabled"
        android:value="true" />
</application>
```

## Initialize the CardScan Client


### Remove CardScanSheet.create()
Remove the code resembling the following simplified example:
```kotlin
// Remove this
val cardScanSheet = CardScanSheet.create(this, object : CardScanSheet.CardScanResultCallback {  
    override fun onCardScanSheetResult(result: CardScanSheetResult) {  
        when (result) {  
            is CardScanSheetResult.Completed -> handleScannedCard(result.scannedCard)  
            // ... handle Canceled/Failed  
        }  
    }  
})  
```

### Add cardRecognitionLauncher
The replacement uses the `PaymentsClient` to launch the recognition flow. Since Google Pay Card Recognition uses the standard Android **Activity Result API**, you should use `registerForActivityResult` for better compatibility and lifecycle handling.
```kotlin
// Add this
private val cardRecognitionLauncher = registerForActivityResult(  
    ActivityResultContracts.StartIntentSenderForResult()  
) { result ->  
    val data = result.data  
    if (result.resultCode == Activity.RESULT_OK && data != null) {  
        val cardDetails = PaymentCardRecognitionResult.getFromIntent(data)  
        cardDetails?.let { handleScannedCard(it) }  
    } else {
        // ... handle Canceled/Failed  
    }  
}
```

## Initialize the PaymentsClient and Launch the Recognition


### Remove CardScanSheet.present() or attachCardScanFragment()
```kotlin
// Remove this
viewBinding.launchScanButton.setOnClickListener {  
    cardScanSheet.present(CardScanConfiguration(null))  
}
// or remove this if using attachCardScanFragment()
viewBinding.launchScanButton.setOnClickListener {
    viewBinding.launchScanButton.isEnabled = false
    viewBinding.fragmentContainer.visibility = View.VISIBLE
    cardScanSheet.attachCardScanFragment(
        this,
        supportFragmentManager,
        R.id.fragment_container,
        this::onScanFinished
    )
}

```

### Replace with paymentsClient launch
```kotlin
// Replace with this
// note: Google Pay Card Recognition always launches a new activity, you'll need to update your UI accordingly if you were using fragments
viewBinding.launchScanButton.setOnClickListener {  
    present()  
}

fun createPaymentsClient(activity: Activity): PaymentsClient {  
    val walletOptions = Wallet.WalletOptions.Builder()  
        // choose between ENVIRONMENT_PRODUCTION and ENVIRONMENT_TEST  
        // ENVIRONMENT_TEST always returns a mock result
        .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)   
        .build()

    return Wallet.getPaymentsClient(activity, walletOptions)  
}

fun present() {  
    val paymentsClient = createPaymentsClient(this)  
    val request = PaymentCardRecognitionIntentRequest.getDefaultInstance()  
    paymentsClient  
        .getPaymentCardRecognitionIntent(request)  
        .addOnSuccessListener { intentResponse ->  
            val pendingIntent = intentResponse.paymentCardRecognitionPendingIntent  
            val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()  
            cardRecognitionLauncher.launch(intentSenderRequest)  
        }  
        .addOnFailureListener { e ->  
            // The API is not available either because the feature is not enabled on the device  
            // or because your app is not registered.  
            Log.e(TAG, "Payment card ocr not available.", e)  
        }  
}
```

## Handle the Scanned Data

Update the function that processes the recognized card data.

### Remove data class

Make sure you removed the old `ScannedCard` class:

```kotlin
// REMOVED CODE  
data class ScannedCard(  
    val pan: String,         // The card number  
    val expiryMonth: Int?,  
    val expiryYear: Int?  
)
```

### Replacement data class

Your app now uses `CardRecognitionResult` from the Google Pay API:

```kotlin
// REPLACEMENT CLASS  
private fun handleScannedCard(result: CardRecognitionResult) {  
    val recognizedCardNumber: String = result.pan  
    val expirationMonth: Int = result.creditCardExpirationDate?.month // 1-12  
    val expirationYear: Int? = result.creditCardExpirationDate?.year  // Four-digit year

    // Your logic to update your UI/backend with the new card details  
}
```
