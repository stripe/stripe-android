package com.stripe.android.stripecardscan.cardscan

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.wallet.PaymentCardRecognitionIntentRequest
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.stripe.android.stripecardscan.cardscan.CardScanSheet.CardScanResultCallback
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason

class CardScanGoogleImpl(
    private val context: ComponentActivity,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>
) {
    constructor(
        activity: ComponentActivity,
        cardScanSheetResultCallback: CardScanResultCallback,
        registry: ActivityResultRegistry
    ) : this(
        activity,
        activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
            registry
        ) { result ->
            val cardScanResult = parseActivityResult(result)
            cardScanSheetResultCallback.onCardScanSheetResult(cardScanResult)
        }
    )
    private fun createPaymentsClient(activity: ComponentActivity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()

        return Wallet.getPaymentsClient(activity, walletOptions)
    }
    fun fetchIntent(onSuccess: (IntentSenderRequest) -> Unit) {
        val paymentsClient = createPaymentsClient(context)
        val request = PaymentCardRecognitionIntentRequest.getDefaultInstance()

        paymentsClient.getPaymentCardRecognitionIntent(request)
            .addOnSuccessListener { intentResponse ->
                val pendingIntent = intentResponse.paymentCardRecognitionPendingIntent
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                onSuccess(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                Log.e("SCANSCAN", "Payment card ocr not available.", e)
            }

    }
    fun launch() {
        fetchIntent { intentSenderRequest ->
            launcher.launch(intentSenderRequest)
        }
    }
}
fun parseActivityResult(result: ActivityResult): CardScanSheetResult {
    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
        val data = result.data ?: return CardScanSheetResult.Canceled(CancellationReason.Closed)
        val paymentCardRecognitionResult = PaymentCardRecognitionResult.getFromIntent(data)
        val pan = paymentCardRecognitionResult?.pan
        return if (pan != null) {
            CardScanSheetResult.Completed(ScannedCard(pan))
        } else {
            CardScanSheetResult.Failed(Throwable("Failed to parse card data"))
        }
    }
    return CardScanSheetResult.Canceled(CancellationReason.Closed)
}
