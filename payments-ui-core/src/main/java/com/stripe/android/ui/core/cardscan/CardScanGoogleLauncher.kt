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
import androidx.compose.runtime.remember
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.parcelize.Parcelize

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

    internal fun parseActivityResult(result: ActivityResult): CardScanSheetResult {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data ?: return CardScanSheetResult.Canceled(CancellationReason.Closed).also {
                eventsReporter.scanCancelled(implementation, CancellationReason.Closed)
            }
            val paymentCardRecognitionResult = PaymentCardRecognitionResult.getFromIntent(data)
            val pan = paymentCardRecognitionResult?.pan
            return if (pan != null) {
                eventsReporter.scanSucceeded(implementation)
                CardScanSheetResult.Completed(ScannedCard(pan))
            } else {
                val error = Throwable("Failed to parse card data")
                eventsReporter.scanFailed("google_pay", error)
                CardScanSheetResult.Failed(error)
            }
        }
        eventsReporter.scanCancelled(implementation, CancellationReason.Closed)
        return CardScanSheetResult.Canceled(CancellationReason.Closed)
    }

    companion object {
        @Composable
        internal fun rememberCardScanGoogleLauncher(
            context: Context,
            eventsReporter: CardScanEventsReporter,
            onResult: (CardScanSheetResult) -> Unit
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
