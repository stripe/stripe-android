package com.stripe.android.common.taptoadd

import androidx.compose.runtime.Composable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.ui.core.elements.CardDetailsAction

internal class TapToAddCardDetailsAction(
    private val tapToAddHelper: TapToAddHelper,
    private val paymentMethodMetadata: PaymentMethodMetadata,
) : CardDetailsAction {
    @Composable
    override fun Content(enabled: Boolean) {
        TapToButtonUI(enabled) {
            tapToAddHelper.startPaymentMethodCollection(paymentMethodMetadata)
        }
    }
}
