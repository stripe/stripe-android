package com.stripe.android.ui.core.cardscan

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CardScanGoogleLauncher @VisibleForTesting constructor(
    context: Context,
    private val paymentCardRecognitionClient: PaymentCardRecognitionClient =
        DefaultPaymentCardRecognitionClient()
) {
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    @VisibleForTesting
    lateinit var activityLauncher: ActivityResultLauncher<IntentSenderRequest>

    init {
        paymentCardRecognitionClient.fetchIntent(
            context = context,
            onFailure = { e ->
                _isAvailable.value = false
            },
            onSuccess = {
                _isAvailable.value = true
            }
        )
    }

    fun launch(context: Context) {
        paymentCardRecognitionClient.fetchIntent(
            context = context,
            onSuccess = { intentSenderRequest ->
                activityLauncher.launch(intentSenderRequest)
            }
        )
    }

    internal fun parseActivityResult(result: ActivityResult): CardScanResult {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data ?: return CardScanResult.Canceled
            val paymentCardRecognitionResult = PaymentCardRecognitionResult.getFromIntent(data)
            val pan = paymentCardRecognitionResult?.pan
            val expirationDate = paymentCardRecognitionResult?.creditCardExpirationDate
            return if (pan != null) {
                CardScanResult.Completed(ScannedCard(pan, expirationDate?.month, expirationDate?.year))
            } else {
                val error = Throwable("Failed to parse card data")
                CardScanResult.Failed(error)
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            return CardScanResult.Canceled
        }
        return CardScanResult.Failed(Throwable("Null data or unexpected result code: ${result.resultCode}"))
    }

    companion object {
        @Composable
        internal fun rememberCardScanGoogleLauncher(
            context: Context,
            onResult: (CardScanResult) -> Unit
        ): CardScanGoogleLauncher {
            val launcher = remember(context) { CardScanGoogleLauncher(context) }
            val activityLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
                onResult(launcher.parseActivityResult(result))
            }
            return remember(activityLauncher) {
                launcher.apply { this.activityLauncher = activityLauncher }
            }
        }
    }
}

internal sealed interface CardScanResult {

    data class Completed(
        val scannedCard: ScannedCard
    ) : CardScanResult

    data object Canceled : CardScanResult

    data class Failed(val error: Throwable) : CardScanResult
}

/**
 * Card details from the scanner
 */
internal data class ScannedCard(
    val pan: String,
    val expirationMonth: Int?,
    val expirationYear: Int?
)
