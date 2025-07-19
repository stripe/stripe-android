package com.stripe.android.stripecardscan.externalscan

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.wallet.PaymentCardRecognitionIntentRequest
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason

class ComposeCardScanner(
    private val context: ComponentActivity,
    private val launcher: (IntentSenderRequest) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    fun launch() {
        val paymentsClient = createPaymentsClient(context)
        val request = PaymentCardRecognitionIntentRequest.getDefaultInstance()

        paymentsClient.getPaymentCardRecognitionIntent(request)
            .addOnSuccessListener { intentResponse ->
                val pendingIntent = intentResponse.paymentCardRecognitionPendingIntent
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                launcher(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Payment card ocr not available.", e)
                onError(e)
            }
    }

    private fun createPaymentsClient(activity: ComponentActivity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()

        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    companion object {
        private const val TAG = "ComposeCardScanner"
    }
}

@Composable
fun rememberGoogleCardScanner(
    onCardScanResult: (CardScanSheetResult) -> Unit,
    onCardScanUnavailable: () -> Unit
): ComposeCardScanner {
    val context = LocalContext.current as ComponentActivity

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val cardScanResult = parseActivityResult(result)
        onCardScanResult(cardScanResult)
    }
    
    return remember(context, launcher) {
        ComposeCardScanner(
            context = context,
            launcher = { intentSenderRequest ->
                launcher.launch(intentSenderRequest)
            },
            onError = {
                onCardScanUnavailable()
            }
        )
    }
}

private fun parseActivityResult(result: ActivityResult): CardScanSheetResult {
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
