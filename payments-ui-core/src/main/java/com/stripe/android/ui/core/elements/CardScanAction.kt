package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.ui.core.cardscan.CardScanLoadingState
import com.stripe.android.ui.core.cardscan.CardScanResult
import com.stripe.android.ui.core.cardscan.rememberCardScanLauncher
import com.stripe.android.uicore.utils.collectAsState

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardScanAction(
    private val isStripeCardScanAllowed: Boolean,
    private val enableMlKitCardScan: Boolean,
    private val disableSsdOcrCardScan: Boolean,
    private val apiConfiguration: ApiConfiguration.State,
    val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
    private val backupAction: CardDetailsAction?,
) : CardDetailsAction {
    @Composable
    override fun Content(enabled: Boolean, onScannedCard: (ScannedCardDetails) -> Unit) {
        val context = LocalContext.current
        val launcher = rememberCardScanLauncher(
            isStripeCardScanAllowed = isStripeCardScanAllowed,
            enableMlKitCardScan = enableMlKitCardScan,
            disableSsdOcrCardScan = disableSsdOcrCardScan,
            apiConfiguration = apiConfiguration,
            onResult = { result ->
                (result as? CardScanResult.Completed)?.scannedCard?.let { scannedCard ->
                    onScannedCard(
                        ScannedCardDetails.Unvalidated(
                            cardNumber = scannedCard.pan,
                            expirationYear = scannedCard.expirationYear,
                            expirationMonth = scannedCard.expirationMonth,
                        )
                    )
                }
            },
        )

        if (launcher == null) {
            backupAction?.Content(
                enabled = enabled,
                onScannedCard = onScannedCard,
            )
            return
        }

        val loadingState by launcher.loadingState.collectAsState()

        when (loadingState) {
            CardScanLoadingState.Loading -> Unit
            CardScanLoadingState.Available -> {
                if (automaticallyLaunchedCardScanFormDataHelper?.shouldLaunchCardScanAutomatically == true) {
                    SideEffect {
                        automaticallyLaunchedCardScanFormDataHelper.hasAutomaticallyLaunchedCardScan = true
                        launcher.launch(context)
                    }
                }

                ScanCardButtonUI(
                    enabled = enabled,
                    cardScanLauncher = launcher,
                )
            }
            CardScanLoadingState.Unavailable -> {
                backupAction?.Content(
                    enabled = enabled,
                    onScannedCard = onScannedCard,
                )
            }
        }
    }
}
