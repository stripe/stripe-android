package com.stripe.android.common.nfcscan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import com.stripe.android.common.taptoadd.TapToButtonUI
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.ui.core.elements.CardDetailsAction
import com.stripe.android.ui.core.elements.ScannedCardDetails

internal class NfcCardAction(val paymentMethodMetadata: PaymentMethodMetadata) : CardDetailsAction {
    @Composable
    override fun Content(
        enabled: Boolean,
        onScannedCard: (ScannedCardDetails) -> Unit
    ) {
        val launcher = rememberLauncherForActivityResult(NfcScanningContract) { result ->
            if (result is NfcScanningContract.Result.Complete) {
                onScannedCard(
                    ScannedCardDetails(
                        cardNumber = result.cardNumber,
                        expirationYear = result.expirationYear,
                        expirationMonth = result.expirationMonth,
                    )
                )
            }
        }

        TapToButtonUI(enabled = enabled) {
            launcher.launch(NfcScanningContract.Args(paymentMethodMetadata.merchantName))
        }
    }
}
