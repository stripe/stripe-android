@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import androidx.annotation.VisibleForTesting
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal interface ExpressCheckoutElementInteractor {
    val state: StateFlow<State>

    data class State(
        val expressButtons: List<ExpressButton>,
    )
}

internal class DefaultExpressCheckoutElementInteractor(
    @get:VisibleForTesting internal val availableExpressButtonTypes: List<WalletType>,
    paymentMethodMetadata: PaymentMethodMetadata,
    linkAccountHolder: LinkAccountHolder,
) : ExpressCheckoutElementInteractor {
    override val state: StateFlow<ExpressCheckoutElementInteractor.State> =
        linkAccountHolder.linkAccountInfo.mapAsStateFlow { linkAccountInfo ->
            ExpressCheckoutElementInteractor.State(
                expressButtons = availableExpressButtonTypes.map { walletType ->
                    when (walletType) {
                        WalletType.Link -> ExpressButton.Link.create(
                            paymentMethodMetadata = paymentMethodMetadata,
                            linkAccountInfo = linkAccountInfo,
                        )
                        WalletType.GooglePay -> ExpressButton.GooglePay.create(
                            paymentMethodMetadata = paymentMethodMetadata,
                        )
                    }
                },
            )
        }

    class Factory @Inject constructor(
        private val linkAccountHolder: LinkAccountHolder,
    ) {
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
                linkAccountHolder = linkAccountHolder,
            )
        }
    }
}
