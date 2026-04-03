package com.stripe.android.ui.core.cardscan

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheet
import com.stripe.android.stripecardscan.cardscan.CardScanSheetParams
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CardScanStripeLauncher(
    context: Context,
    private val eventsReporter: CardScanEventsReporter,
    private val enableMlKitCardScan: Boolean,
    private val elementsSessionId: String?,
    isLaunchingState: MutableState<Boolean>,
) : CardScanLauncher {

    private val implementation = "stripe_card_scan"
    private var _isLaunching by isLaunchingState

    // CardScanSheet.isSupported is safe to call directly here because this class is only
    // instantiated after confirming stripecardscan is available at runtime (see rememberCardScanLauncher).
    private val _isAvailable = MutableStateFlow(CardScanSheet.isSupported(context))
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    lateinit var activityLauncher: ActivityResultLauncher<CardScanSheetParams>

    override fun launch(context: Context) {
        if (_isLaunching) {
            return
        }

        _isLaunching = true
        eventsReporter.onCardScanStarted(implementation)
        activityLauncher.launch(
            CardScanSheetParams(
                CardScanConfiguration(
                    elementsSessionId = elementsSessionId,
                    enableMlKitTextRecognition = enableMlKitCardScan,
                )
            )
        )
    }

    internal fun parseActivityResult(intent: Intent?): CardScanResult {
        val sheetResult: CardScanSheetResult = intent?.let {
            IntentCompat.getParcelableExtra(it, INTENT_PARAM_RESULT, CardScanSheetResult::class.java)
        } ?: CardScanSheetResult.Failed(UnknownScanException("No data in the result intent"))

        val scanResult = when (sheetResult) {
            is CardScanSheetResult.Completed -> {
                CardScanResult.Completed(
                    ScannedCard(
                        pan = sheetResult.scannedCard.pan,
                        expirationMonth = sheetResult.scannedCard.expiryMonth,
                        expirationYear = sheetResult.scannedCard.expiryYear,
                    )
                )
            }
            is CardScanSheetResult.Canceled -> CardScanResult.Canceled
            is CardScanSheetResult.Failed -> CardScanResult.Failed(sheetResult.error)
        }

        when (scanResult) {
            is CardScanResult.Completed -> eventsReporter.onCardScanSucceeded(implementation)
            is CardScanResult.Canceled -> eventsReporter.onCardScanCancelled(implementation)
            is CardScanResult.Failed -> eventsReporter.onCardScanFailed(implementation, scanResult.error)
        }

        return scanResult
    }

    companion object {
        private const val INTENT_PARAM_REQUEST = "request"
        private const val INTENT_PARAM_RESULT = "result"

        private val cardScanActivityClass: Class<*> by lazy {
            Class.forName("com.stripe.android.stripecardscan.cardscan.CardScanActivity")
        }

        private val activityResultContract = object : ActivityResultContract<
            CardScanSheetParams,
            Pair<Int, Intent?>
            >() {
            override fun createIntent(context: Context, input: CardScanSheetParams): Intent {
                return Intent(context, cardScanActivityClass)
                    .putExtra(INTENT_PARAM_REQUEST, input)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Pair<Int, Intent?> {
                return resultCode to intent
            }
        }

        @Composable
        internal fun rememberCardScanStripeLauncher(
            eventsReporter: CardScanEventsReporter,
            enableMlKitCardScan: Boolean = false,
            elementsSessionId: String? = null,
            onResult: (CardScanResult) -> Unit,
        ): CardScanStripeLauncher {
            val context = LocalContext.current.applicationContext
            val isLaunchingState = rememberSaveable { mutableStateOf(false) }
            val launcher = remember(eventsReporter, context, enableMlKitCardScan, elementsSessionId) {
                CardScanStripeLauncher(
                    context = context,
                    eventsReporter = eventsReporter,
                    enableMlKitCardScan = enableMlKitCardScan,
                    elementsSessionId = elementsSessionId,
                    isLaunchingState = isLaunchingState,
                )
            }
            val activityLauncher = rememberLauncherForActivityResult(
                activityResultContract,
            ) { (_, intent) ->
                launcher._isLaunching = false
                onResult(launcher.parseActivityResult(intent))
            }
            return remember(activityLauncher) {
                launcher.apply { this.activityLauncher = activityLauncher }
            }
        }
    }
}
