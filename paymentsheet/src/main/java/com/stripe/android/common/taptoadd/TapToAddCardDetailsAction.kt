package com.stripe.android.common.taptoadd

import androidx.compose.runtime.Composable
import com.stripe.android.ui.core.elements.CardDetailsAction

internal class TapToAddCardDetailsAction(
    private val tapToAddHelper: TapToAddHelper,
) : CardDetailsAction {
    @Composable
    override fun Content(enabled: Boolean) {
        TapToButtonUI(enabled) {
            tapToAddHelper.startPaymentMethodCollection()
        }
    }
}
