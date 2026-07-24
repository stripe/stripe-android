package com.stripe.android.ui.core.cardscan

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
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

internal class CardScanGoogleLauncher @VisibleForTesting constructor(
    context: Context,
    private val options: ActivityOptionsCompat?,
    private val eventsReporter: CardScanEventsReporter,
    private val paymentCardRecognitionClient: PaymentCardRecognitionClient
) : CardScanLauncher {
    private val implementation = "google_pay"
    private var _isLaunching = false
    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    @VisibleForTesting
    var activityLauncher: ActivityResultLauncher<Unit>? = null

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

    override fun launch(context: Context) {
        if (_isLaunching) {
            // Prevent multiple simultaneous launches
            return
        }

        val launcher = activityLauncher ?: return
        _isLaunching = true
        eventsReporter.onCardScanStarted(implementation)
        runCatching {
            launcher.launch(Unit, options)
        }.onFailure { e ->
            _isLaunching = false
            eventsReporter.onCardScanFailed(implementation, e)
        }
    }

    internal fun parseActivityResult(result: ActivityResult): CardScanResult {
        val scanResult = when {
            result.resultCode == CardScanGoogleActivity.RESULT_CARD_SCAN_FAILED -> {
                CardScanResult.Failed(
                    CardScanActivityResultException(
                        CardScanGoogleActivity.getErrorMessage(result.data)
                            ?: "Failed to start Google Pay card recognition"
                    )
                )
            }
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
        private val activityResultContract = object : ActivityResultContract<Unit, ActivityResult>() {
            override fun createIntent(context: Context, input: Unit): Intent {
                return CardScanGoogleActivity.createIntent(context)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
                return ActivityResult(resultCode, intent)
            }
        }

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
                activityResultContract,
            ) { result ->
                launcher._isLaunching = false
                onResult(launcher.parseActivityResult(result))
            }
            val registeredLauncher = remember(launcher, activityLauncher) {
                launcher.apply { this.activityLauncher = activityLauncher }
            }
            DisposableEffect(launcher, activityLauncher) {
                launcher.activityLauncher = activityLauncher
                onDispose {
                    if (launcher.activityLauncher === activityLauncher) {
                        launcher.activityLauncher = null
                    }
                }
            }
            return registeredLauncher
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
