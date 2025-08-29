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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityOptionsCompat
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CardScanGoogleLauncher @VisibleForTesting constructor(
    context: Context,
    private val options: ActivityOptionsCompat?,
    private val eventsReporter: CardScanEventsReporter,
    private val paymentCardRecognitionClient: PaymentCardRecognitionClient
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
                eventsReporter.onCardScanApiCheckFailed(implementation, e)
            },
            onSuccess = {
                _isAvailable.value = true
                eventsReporter.onCardScanApiCheckSucceeded(implementation)
            }
        )
    }

    fun launch(context: Context) {
        paymentCardRecognitionClient.fetchIntent(
            context = context,
            onFailure = { e ->
                eventsReporter.onCardScanFailed(implementation, e)
            },
            onSuccess = { intentSenderRequest ->
                eventsReporter.onCardScanStarted("google_pay")
                activityLauncher.launch(intentSenderRequest, options)
            }
        )
    }

    internal fun parseActivityResult(result: ActivityResult): CardScanResult {
        val scanResult = when {
            result.resultCode == Activity.RESULT_OK && result.data != null -> {
                val data = result.data ?: return CardScanResult.Canceled
                val paymentCardRecognitionResult = PaymentCardRecognitionResult.getFromIntent(data)
                val pan = paymentCardRecognitionResult?.pan
                val expirationDate = paymentCardRecognitionResult?.creditCardExpirationDate
                if (pan != null) {
                    CardScanResult.Completed(ScannedCard(pan, expirationDate?.month, expirationDate?.year))
                } else {
                    CardScanResult.Failed(
                        CardScanParseException("PAN not found in PaymentCardRecognitionResult")
                    )
                }
            }
            result.resultCode == Activity.RESULT_CANCELED -> {
                CardScanResult.Canceled
            }
            else -> {
                CardScanResult.Failed(
                    CardScanActivityResultException(
                        "Invalid activity result: code=${result.resultCode}, hasData=${result.data != null}"
                    )
                )
            }
        }

        // Report events based on the result
        when (scanResult) {
            is CardScanResult.Completed -> eventsReporter.onCardScanSucceeded(implementation)
            is CardScanResult.Canceled -> eventsReporter.onCardScanCancelled(implementation)
            is CardScanResult.Failed -> eventsReporter.onCardScanFailed(implementation, scanResult.error)
        }

        return scanResult
    }

    companion object {
        @Composable
        internal fun rememberCardScanGoogleLauncher(
            context: Context,
            eventsReporter: CardScanEventsReporter,
            options: ActivityOptionsCompat? = null,
            onResult: (CardScanResult) -> Unit
        ): CardScanGoogleLauncher {
            val paymentCardRecognitionClient = LocalPaymentCardRecognitionClient.current
            val launcher = remember(context, options, eventsReporter, paymentCardRecognitionClient) {
                CardScanGoogleLauncher(
                    context,
                    options,
                    eventsReporter,
                    paymentCardRecognitionClient
                )
            }
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

@VisibleForTesting
internal val LocalPaymentCardRecognitionClient = compositionLocalOf<PaymentCardRecognitionClient> {
    DefaultPaymentCardRecognitionClient()
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

/**
 * Exception thrown when card scan data parsing fails
 */
internal class CardScanParseException(message: String) : Exception(message)

/**
 * Exception thrown when activity result is invalid
 */
internal class CardScanActivityResultException(message: String) : Exception(message)
