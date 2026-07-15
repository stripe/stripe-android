@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal interface ExpressCheckoutElementInteractor {
    val state: StateFlow<State>

    data class State(
        val expressButtons: List<ExpressButton>,
    )
}

internal class DefaultExpressCheckoutElementInteractor(
    availableExpressButtons: List<ExpressButton>,
) : ExpressCheckoutElementInteractor {
    override val state: StateFlow<ExpressCheckoutElementInteractor.State> = stateFlowOf(
        ExpressCheckoutElementInteractor.State(
            expressButtons = availableExpressButtons,
        )
    )

    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfig: ExpressCheckoutElement.Configuration.State,
    ): DefaultExpressCheckoutElementInteractor {
        return DefaultExpressCheckoutElementInteractor(
            availableExpressButtons = paymentMethodMetadata.availableWallets.mapNotNull { walletType ->
                when (walletType) {
                    WalletType.GooglePay -> ExpressButton.GooglePay.takeIf {
                        expressCheckoutElementConfig.googlePayVisibility !=
                            ExpressCheckoutElement.Configuration.GooglePayVisibility.Never
                    }
                    WalletType.Link -> ExpressButton.Link.takeIf {
                        expressCheckoutElementConfig.linkVisibility !=
                            ExpressCheckoutElement.Configuration.LinkVisibility.Never
                    }
                }
            }
        )
    }
}
