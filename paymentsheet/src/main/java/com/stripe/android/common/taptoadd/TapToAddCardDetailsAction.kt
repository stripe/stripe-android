package com.stripe.android.common.taptoadd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
        var buttonReportedShown by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (!buttonReportedShown) {
                tapToAddHelper.reportButtonShown()
                buttonReportedShown = true
            }
        }

        TapToButtonUI(enabled = enabled && isTapToAddEnabled) {
            tapToAddHelper.startPaymentMethodCollection(paymentMethodMetadata)
        }
    }
}
