package com.stripe.android.stripecardscan.cardscan

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.wallet.PaymentCardRecognitionIntentRequest
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardScanGoogleLauncher(
    context: Context,
    private val activityLauncher: ActivityResultLauncher<IntentSenderRequest>
) {
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    init {
        fetchIntent(context) {
            _isAvailable.value = true
        }
    }

    private fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(
                if (isPaymentSheetExample)
                    WalletConstants.ENVIRONMENT_TEST
                else
                    WalletConstants.ENVIRONMENT_PRODUCTION
            )
            .build()

        return Wallet.getPaymentsClient(context, walletOptions)
    }

    private fun fetchIntent(context: Context, onSuccess: (IntentSenderRequest) -> Unit) {
        val paymentsClient = createPaymentsClient(context)
        val request = PaymentCardRecognitionIntentRequest.getDefaultInstance()

        paymentsClient.getPaymentCardRecognitionIntent(request)
            .addOnSuccessListener { intentResponse ->
                val pendingIntent = intentResponse.paymentCardRecognitionPendingIntent
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                onSuccess(intentSenderRequest)
            }
            .addOnFailureListener { e ->
            }
    }

    fun launch(context: Context) {
        fetchIntent(context) { intentSenderRequest ->
            activityLauncher.launch(intentSenderRequest)
        }
    }

    companion object {
        @Composable
        fun rememberCardScanGoogleLauncher(
            context: Context,
            onResult: (CardScanSheetResult) -> Unit
        ): CardScanGoogleLauncher {
            val activityLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult(),
            ) {
                onResult(parseActivityResult(it))
            }
            return remember(activityLauncher) {
                CardScanGoogleLauncher(context, activityLauncher)
            }
        }

        var isPaymentSheetExample = false
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