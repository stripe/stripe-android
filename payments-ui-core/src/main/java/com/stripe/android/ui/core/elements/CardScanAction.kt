package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.ui.core.cardscan.CardScanResult
import com.stripe.android.ui.core.cardscan.rememberCardScanLauncher

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardScanAction(
    private val isStripeCardScanAllowed: Boolean,
    private val enableMlKitCardScan: Boolean,
    private val disableSsdOcrCardScan: Boolean,
    val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
) : CardDetailsAction {
    @Composable
    override fun Content(enabled: Boolean, onScannedCard: (ScannedCardDetails) -> Unit) {
        val context = LocalContext.current
        val launcher = rememberCardScanLauncher(
            isStripeCardScanAllowed = isStripeCardScanAllowed,
            enableMlKitCardScan = enableMlKitCardScan,
            disableSsdOcrCardScan = disableSsdOcrCardScan,
            onResult = { result ->
                (result as? CardScanResult.Completed)?.scannedCard?.let { scannedCard ->
                    onScannedCard(
                        ScannedCardDetails(
                            cardNumber = scannedCard.pan,
                            expirationYear = scannedCard.expirationYear,
                            expirationMonth = scannedCard.expirationMonth,
                        )
                    )
                }
            },
        )

        if (
            automaticallyLaunchedCardScanFormDataHelper?.shouldLaunchCardScanAutomatically == true &&
            launcher != null
        ) {
            SideEffect {
                automaticallyLaunchedCardScanFormDataHelper.hasAutomaticallyLaunchedCardScan = true
                launcher.launch(context)
            }
        }

        ScanCardButtonUI(enabled = enabled, cardScanLauncher = launcher)
    }
}
