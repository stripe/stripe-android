# Migrating from Stripe CardScan to Google Pay Card Recognition API

> **⚠️ Important: Opt-in Required**
>
> Before you start migrating, you must **opt-in by [requesting production access](https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access) to the Google Pay API** using the [Google Pay & Wallet Console](https://pay.google.com/business/console?utm_source=devsite&utm_medium=devsite&utm_campaign=devsite).

---

Migrating your code from the removed Stripe CardScan module to the Google Pay Card Recognition API involves three main steps: updating dependencies, changing the initialization logic, and replacing the result handling.

Here is a general guide on how to replace the removed Stripe classes with the Google Pay functionality.

## Step 1: Update Dependencies

First, remove the old Stripe CardScan dependency and add the necessary Google Play service dependency to your app-level `build.gradle` (or `build.gradle.kts`):

### Remove Stripe CardScan Dependency

Remove the line for the stripecardscan module:

```groovy
// REMOVE THIS LINE  
implementation("com.stripe:stripecardscan:VERSION")
```

### Add Google Pay Dependency

Add the [Google Play service library](https://developers.google.com/pay/api/android/guides/setup#app%20dependencies):

```groovy
implementation 'com.google.android.gms:play-services-wallet:VERSION' // Use the latest stable version
```

Enable Google Pay API in `AndroidManifest.xml`:

```xml
<application>
    <meta-data
        android:name="com.google.android.gms.wallet.api.enabled"
        android:value="true" />
</application>
```

---

## Step 2: Implement Google Pay Card Recognition

Replace your calls to `CardScanSheet.create()` and `present()` with the Google Pay API calls.
> **Note**: `attachCardScanFragment()` is not supported in Google Pay Card Recognition.

### Remove Stripe CardScan Logic

Your old code likely looked something like this (simplified):

```kotlin
// REMOVED CODE  
val cardScanSheet = CardScanSheet.create(this, object : CardScanSheet.CardScanResultCallback {  
    override fun onCardScanSheetResult(result: CardScanSheetResult) {  
        when (result) {  
            is CardScanSheetResult.Completed -> handleScannedCard(result.scannedCard)  
            // ... handle Canceled/Failed  
        }  
    }  
})  

viewBinding.launchScanButton.setOnClickListener {  
    cardScanSheet.present(CardScanConfiguration(null))  
}
```

### Replace with Google Pay Logic

The replacement uses the `PaymentsClient` to launch the recognition flow. Since Google Pay Card Recognition uses the standard Android **Activity Result API**, you should use `registerForActivityResult` for better compatibility and lifecycle handling.

#### 1. Create a Result Launcher (in your Fragment or Activity):

```kotlin
// 1. Define the launcher for the recognition result  
private val cardRecognitionLauncher = registerForActivityResult(  
    ActivityResultContracts.StartIntentSenderForResult()  
) { result ->  
    val data = result.data  
    if (result.resultCode == Activity.RESULT_OK && data != null) {  
        // Successful scan  
        val cardDetails = PaymentCardRecognitionResult.getFromIntent(data)  
        cardDetails?.let { handleScannedCard(it) }  
    } else {  
        // Scan failed or was canceled  
        // You can check result.resultCode for specific error/cancellation codes  
        handleScanFailure()  
    }  
}
```

#### 2. Initialize the PaymentsClient and Launch the Recognition:

```kotlin
viewBinding.launchScanButton.setOnClickListener {  
    present()  
}
```

When the user taps the "Scan Card" button (or equivalent):

```kotlin
// 2. Initialize the PaymentsClient  
fun createPaymentsClient(activity: Activity): PaymentsClient {  
    val walletOptions = Wallet.WalletOptions.Builder()  
        // choose between ENVIRONMENT_PRODUCTION and ENVIRONMENT_TEST  
        // ENVIRONMENT_TEST always returns a mock result
        .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)   
        .build()

    return Wallet.getPaymentsClient(activity, walletOptions)  
}

// 3. Launch the recognition flow  
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

---

## Step 3: Handle the Scanned Data

The final step is to update the function that processes the recognized card data.

### Old Data Class (Removed)

The old `ScannedCard` class has been removed:

```kotlin
// REMOVED CODE  
data class ScannedCard(  
    val pan: String,         // The card number  
    val expiryMonth: Int?,  
    val expiryYear: Int?  
)
```

### New Data Class (Replacement)

You will now use `CardRecognitionResult` from the Google Pay API:

```kotlin
// REPLACEMENT CLASS  
private fun handleScannedCard(result: CardRecognitionResult) {  
    val recognizedCardNumber: String = result.pan  
    val expirationMonth: Int = result.creditCardExpirationDate?.month // 1-12  
    val expirationYear: Int? = result.creditCardExpirationDate?.year  // Four-digit year

    // Your logic to update your UI/backend with the new card details  
}
```

---

## Summary

By following these three steps—updating the build files, replacing the launch logic using `PaymentsClient` and `registerForActivityResult`, and handling the `CardRecognitionResult`—you will have successfully migrated your card scanning functionality from the removed Stripe CardScan module to the Google Pay Card Recognition API.