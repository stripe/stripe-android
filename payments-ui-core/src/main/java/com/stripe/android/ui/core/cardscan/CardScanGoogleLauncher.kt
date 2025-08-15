package com.stripe.android.ui.core.cardscan

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CardScanGoogleLauncher(
    context: Context,
    private val eventsReporter: CardScanEventsReporter,
    private val paymentCardRecognitionClient: PaymentCardRecognitionClient =
        DefaultPaymentCardRecognitionClient()
) {
    private val implementation = "google_pay"
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private lateinit var activityLauncher: ActivityResultLauncher<IntentSenderRequest>

    init {
        paymentCardRecognitionClient.fetchIntent(
            context = context,
            onFailure = { e ->
                _isAvailable.value = false
                eventsReporter.apiCheck(implementation, false, e.message)
            },
            onSuccess = {
                _isAvailable.value = true
                eventsReporter.apiCheck(implementation, true)
            }
        )
    }

    fun launch(context: Context) {
        eventsReporter.scanStarted("google_pay")
        paymentCardRecognitionClient.fetchIntent(context) { intentSenderRequest ->
            activityLauncher.launch(intentSenderRequest)
        }
    }

    internal fun parseActivityResult(result: ActivityResult): CardScanResult {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data ?: return CardScanResult.Canceled.also {
                eventsReporter.scanCancelled(implementation)
            }
            val paymentCardRecognitionResult = PaymentCardRecognitionResult.getFromIntent(data)
            val pan = paymentCardRecognitionResult?.pan
            return if (pan != null) {
                eventsReporter.scanSucceeded(implementation)
                CardScanResult.Completed(ScannedCard(pan))
            } else {
                val error = Throwable("Failed to parse card data")
                eventsReporter.scanFailed("google_pay", error)
                CardScanResult.Failed(error)
            }
        }
        eventsReporter.scanCancelled(implementation)
        return CardScanResult.Canceled
    }

    companion object {
        @Composable
        internal fun rememberCardScanGoogleLauncher(
            context: Context,
            eventsReporter: CardScanEventsReporter,
            onResult: (CardScanResult) -> Unit
        ): CardScanGoogleLauncher {
            val launcher = remember(context, eventsReporter) { CardScanGoogleLauncher(context, eventsReporter) }
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
    val pan: String
)
