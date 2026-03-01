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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityOptionsCompat
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

internal class CardScanGoogleLauncher @VisibleForTesting constructor(
    context: Context,
    private val options: ActivityOptionsCompat?,
    eventsReporter: CardScanEventsReporter,
    private val paymentCardRecognitionClient: PaymentCardRecognitionClient
) {
    private val implementation = "google_pay"
    private var _isLaunching = false
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    // Use WeakReference to avoid memory leaks when GMS SDK holds lambdas that capture the launcher.
    // The GMS SDK may hold TaskCompletionSource callbacks longer than expected, which would
    // otherwise keep the eventsReporter (and thus the ViewModel) alive after Activity destruction.
    private val eventsReporterRef = WeakReference(eventsReporter)

    @VisibleForTesting
    var activityLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    init {
        paymentCardRecognitionClient.fetchIntent(
            context = context,
            onFailure = { e ->
                _isAvailable.value = false
                eventsReporterRef.get()?.onCardScanApiCheckFailed(implementation, e)
            },
            onSuccess = {
                _isAvailable.value = true
                eventsReporterRef.get()?.onCardScanApiCheckSucceeded(implementation)
            }
        )
    }

    fun launch(context: Context) {
        val launcher = activityLauncher ?: return

        if (_isLaunching) {
            // Prevent multiple simultaneous launches
            return
        }

        _isLaunching = true
        paymentCardRecognitionClient.fetchIntent(
            context = context,
            onFailure = { e ->
                _isLaunching = false
                eventsReporterRef.get()?.onCardScanFailed(implementation, e)
            },
            onSuccess = { intentSenderRequest ->
                eventsReporterRef.get()?.onCardScanStarted("google_pay")
                launcher.launch(intentSenderRequest, options)
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
        eventsReporterRef.get()?.let { reporter ->
            when (scanResult) {
                is CardScanResult.Completed -> reporter.onCardScanSucceeded(implementation)
                is CardScanResult.Canceled -> reporter.onCardScanCancelled(implementation)
                is CardScanResult.Failed -> reporter.onCardScanFailed(implementation, scanResult.error)
            }
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
                launcher._isLaunching = false
                onResult(launcher.parseActivityResult(result))
            }

            // Clean up references when the composable leaves composition to prevent memory leaks.
            // The GMS Wallet SDK may hold references to callbacks that capture the launcher,
            // which in turn holds references to the Activity's ActivityResultRegistry.
            DisposableEffect(launcher) {
                onDispose {
                    launcher.activityLauncher = null
                    launcher._isLaunching = false
                }
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
