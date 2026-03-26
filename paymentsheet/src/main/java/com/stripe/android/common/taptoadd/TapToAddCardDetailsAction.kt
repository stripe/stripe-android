package com.stripe.android.common.taptoadd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.ui.core.elements.CardDetailsAction
import com.stripe.android.uicore.utils.collectAsState

internal class TapToAddCardDetailsAction(
    private val tapToAddHelper: TapToAddHelper,
    private val paymentMethodMetadata: PaymentMethodMetadata,
) : CardDetailsAction {
    @Composable
    override fun Content(enabled: Boolean) {
        val isTapToAddEnabled by tapToAddHelper.isTapToAddEnabled.collectAsState()

        TapToButtonUI(enabled = enabled && isTapToAddEnabled) {
            tapToAddHelper.startPaymentMethodCollection(paymentMethodMetadata)
        }
    }
}
