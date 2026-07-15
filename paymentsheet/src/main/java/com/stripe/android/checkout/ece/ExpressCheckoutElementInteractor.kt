@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import androidx.annotation.VisibleForTesting
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
    @VisibleForTesting internal val availableExpressButtonTypes: List<WalletType>,
    paymentMethodMetadata: PaymentMethodMetadata,
) : ExpressCheckoutElementInteractor {
    override val state: StateFlow<ExpressCheckoutElementInteractor.State> = stateFlowOf(
        ExpressCheckoutElementInteractor.State(
            expressButtons = availableExpressButtonTypes.map { walletType ->
                when (walletType) {
                    WalletType.Link -> ExpressButton.Link.create(
                        paymentMethodMetadata = paymentMethodMetadata,
                    )
                    WalletType.GooglePay -> ExpressButton.GooglePay.create(
                        paymentMethodMetadata = paymentMethodMetadata,
                    )
                }
            },
        )
    )

    companion object {
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata,
            expressCheckoutElementConfig: ExpressCheckoutElement.Configuration.State,
        ): DefaultExpressCheckoutElementInteractor {
            return DefaultExpressCheckoutElementInteractor(
                availableExpressButtonTypes = paymentMethodMetadata.availableWallets.mapNotNull { walletType ->
                    when (walletType) {
                        WalletType.GooglePay -> WalletType.GooglePay.takeIf {
                            expressCheckoutElementConfig.googlePayVisibility !=
                                ExpressCheckoutElement.Configuration.GooglePayVisibility.Never
                        }
                        WalletType.Link -> WalletType.Link.takeIf {
                            expressCheckoutElementConfig.linkVisibility !=
                                ExpressCheckoutElement.Configuration.LinkVisibility.Never
                        }
                    }
                },
                paymentMethodMetadata = paymentMethodMetadata,
            )
        }
    }
}
