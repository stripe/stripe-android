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

internal class CardScanGoogleLauncher(
    context: Context,
    private val eventsReporter: CardScanEventsReporter,
    private val paymentCardRecognitionClient: PaymentCardRecognitionClient =
        DefaultPaymentCardRecognitionClient()
) {
    private val implementation = "google_pay"
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    @VisibleForTesting
    lateinit var activityLauncher: ActivityResultLauncher<IntentSenderRequest>

    init {
        paymentCardRecognitionClient.fetchIntent(
            context = context,
            onFailure = { e ->
                _isAvailable.value = false
                eventsReporter.onCardScanApiCheck(implementation, false, e.message)
            },
            onSuccess = {
                _isAvailable.value = true
                eventsReporter.onCardScanApiCheck(implementation, true)
            }
        )
    }

    fun launch(context: Context) {
        paymentCardRecognitionClient.fetchIntent(
            context = context,
            onFailure = { e ->
                eventsReporter.onCardScanFailed("google_pay", e)
            },
            onSuccess = { intentSenderRequest ->
                eventsReporter.onCardScanStarted("google_pay")
                activityLauncher.launch(intentSenderRequest)
            }
        )
    }

    internal fun parseActivityResult(result: ActivityResult): CardScanResult {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data ?: return CardScanResult.Canceled.also {
                eventsReporter.onCardScanCancelled(implementation)
            }
            val paymentCardRecognitionResult = PaymentCardRecognitionResult.getFromIntent(data)
            val pan = paymentCardRecognitionResult?.pan
            return if (pan != null) {
                eventsReporter.onCardScanSucceeded(implementation)
                CardScanResult.Completed(ScannedCard(pan))
            } else {
                val error = Throwable("Failed to parse card data")
                eventsReporter.onCardScanFailed("google_pay", error)
                CardScanResult.Failed(error)
            }
        }
        eventsReporter.onCardScanCancelled(implementation)
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
