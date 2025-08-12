package com.stripe.android.ui.core.cardscan

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.android.gms.wallet.PaymentCardRecognitionIntentRequest
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.parcelize.Parcelize

class CardScanGoogleLauncher(
    context: Context,
    private val activityLauncher: ActivityResultLauncher<IntentSenderRequest>
) {
    private var _isAvailable = mutableStateOf(false)
    val isAvailable = _isAvailable

    init {
        fetchIntent(context) {
            _isAvailable.value = true
        }
    }

    private fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
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
    }
}

sealed interface CardScanSheetResult : Parcelable {

    @Parcelize
    data class Completed(
        val scannedCard: ScannedCard
    ) : CardScanSheetResult

    @Parcelize
    data class Canceled(
        val reason: CancellationReason
    ) : CardScanSheetResult

    @Parcelize
    data class Failed(val error: Throwable) : CardScanSheetResult
}

/**
 * Card details from the scanner
 */
@Parcelize
data class ScannedCard(
    val pan: String
) : Parcelable

sealed interface CancellationReason : Parcelable {

    @Parcelize
    data object Closed : CancellationReason

    @Parcelize
    data object Back : CancellationReason

    @Parcelize
    data object UserCannotScan : CancellationReason

    @Parcelize
    data object CameraPermissionDenied : CancellationReason
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
