package com.stripe.android.common.nfcscan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import com.stripe.android.common.taptoadd.TapToButtonUI
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.ui.core.elements.CardDetailsAction
import com.stripe.android.ui.core.elements.ScannedCardDetails
import com.stripe.android.uicore.utils.AnimationConstants

internal class NfcScanningAction(
    private val paymentMethodMetadata: PaymentMethodMetadata,
) : CardDetailsAction {
    @Composable
    override fun Content(
        enabled: Boolean,
        onScannedCard: (ScannedCardDetails) -> Unit
    ) {
        val context = LocalContext.current
        val reportNfcScanButtonShown = LocalNfcScanEventShownReporter.current
        var buttonReportedShown by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (!buttonReportedShown) {
                reportNfcScanButtonShown()
                buttonReportedShown = true
            }
        }

        val launcher = rememberLauncherForActivityResult(NfcScanningContract) { result ->
            if (result is NfcScanningContract.Result.Complete) {
                onScannedCard(
                    ScannedCardDetails.Validated(
                        cardNumber = result.cardNumber,
                        expirationYear = result.expirationYear,
                        expirationMonth = result.expirationMonth,
                    ),
                )
            }
        }

        TapToButtonUI(enabled = enabled) {
            launcher.launch(
                NfcScanningContract.Args(paymentMethodMetadata),
                ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    AnimationConstants.FADE_IN,
                    AnimationConstants.FADE_OUT,
                ),
            )
        }
    }
}
