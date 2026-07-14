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

    state.walletButtons.forEach { button ->
        when (button) {
            is ExpressCheckoutElementInteractor.ExpressButton.GooglePay -> GooglePayButton(
                state = PrimaryButton.State.Ready,
                allowCreditCards = button.allowCreditCards,
                buttonType = button.googlePayButtonType,
                billingAddressParameters = button.billingAddressParameters,
                isEnabled = state.buttonsEnabled,
                cardBrandFilter = button.cardBrandFilter,
                cardFundingFilter = button.cardFundingFilter,
                additionalEnabledNetworks = button.additionalEnabledNetworks,
                onPressed = {
                    interactor.handleViewAction(
                        ExpressCheckoutElementInteractor.ViewAction.OnButtonPressed(button)
                    )
                },
            )
            is ExpressCheckoutElementInteractor.ExpressButton.Link -> LinkButton(
                state = button.state,
                enabled = state.buttonsEnabled,
                theme = button.theme,
                linkBrand = button.linkBrand,
                onClick = {
                    interactor.handleViewAction(
                        ExpressCheckoutElementInteractor.ViewAction.OnButtonPressed(button)
                    )
                },
            )
        }
    }
}
