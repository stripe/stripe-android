package com.stripe.android.checkout.ece

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun ExpressCheckoutElementContent(
    interactor: ExpressCheckoutElementInteractor,
) {
    val state by interactor.state.collectAsState()

    state.expressButtons.forEach { button ->
        when (button) {
            is ExpressButton.GooglePay -> GooglePayButton(
                state = PrimaryButton.State.Ready,
                allowCreditCards = button.allowCreditCards,
                buttonType = button.googlePayButtonType,
                billingAddressParameters = button.billingAddressParameters,
                isEnabled = true,
                cardBrandFilter = button.cardBrandFilter,
                cardFundingFilter = button.cardFundingFilter,
                additionalEnabledNetworks = button.additionalEnabledNetworks,
                onPressed = {},
            )
            is ExpressButton.Link -> LinkButton(
                state = button.state,
                enabled = true,
                theme = button.theme,
                linkBrand = button.linkBrand,
                onClick = {},
            )
        }
    }
}
