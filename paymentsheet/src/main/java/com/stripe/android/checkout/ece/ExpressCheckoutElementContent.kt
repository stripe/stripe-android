@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout.ece

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun ExpressCheckoutElementContent(
    interactor: ExpressCheckoutElementInteractor,
) {
    val state by interactor.state.collectAsState()

    LaunchedEffect(Unit) {
        interactor.handleViewAction(ExpressCheckoutElementInteractor.ViewAction.OnDisplayed)
    }

    val buttonModifier = state.buttonHeight?.let { Modifier.height(it.dp) } ?: Modifier
    val content: @Composable () -> Unit = {
        state.expressButtons.forEach { button ->
            key(button) {
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
                        modifier = buttonModifier,
                        onPressed = {
                            interactor.handleViewAction(
                                ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped(
                                    expressButton = button,
                                )
                            )
                        },
                    )
                    is ExpressButton.Link -> LinkButton(
                        state = button.state,
                        enabled = true,
                        theme = button.theme,
                        linkBrand = button.linkBrand,
                        modifier = buttonModifier,
                        onClick = {
                            interactor.handleViewAction(
                                ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped(
                                    expressButton = button,
                                )
                            )
                        },
                    )
                }
            }
        }
    }

    when (state.buttonOrientation) {
        ExpressCheckoutElement.Configuration.ButtonOrientation.Horizontal -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
        ExpressCheckoutElement.Configuration.ButtonOrientation.Vertical -> Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}
