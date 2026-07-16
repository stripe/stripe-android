@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.ece

import androidx.annotation.VisibleForTesting
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.collections.emptyList

internal interface ExpressCheckoutElementInteractor {
    val state: StateFlow<State>

    data class State(
        val expressButtons: List<ExpressButton>,
    )
}

internal class DefaultExpressCheckoutElementInteractor @Inject constructor(
    linkAccountHolder: LinkAccountHolder,
    stateHolder: CheckoutControllerStateHolder,
) : ExpressCheckoutElementInteractor {

    override val state: StateFlow<ExpressCheckoutElementInteractor.State> = combineAsStateFlow(
        linkAccountHolder.linkAccountInfo,
        stateHolder.paymentMethodMetadata,
        stateHolder.configuration,
    ) { linkAccountInfo, paymentMethodMetadata, configuration ->
        if (paymentMethodMetadata == null || configuration == null) {
            return@combineAsStateFlow ExpressCheckoutElementInteractor.State(expressButtons = emptyList())
        }

        val availableExpressButtonTypes = computeAvailableExpressButtonTypes(
            paymentMethodMetadata = paymentMethodMetadata,
            expressCheckoutElementConfiguration = configuration.expressCheckoutElementConfiguration,
        )

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

    @VisibleForTesting
    fun computeAvailableExpressButtonTypes(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
    ): List<WalletType> {
        return paymentMethodMetadata.availableWallets.mapNotNull { walletType ->
            when (walletType) {
                WalletType.GooglePay -> WalletType.GooglePay.takeIf {
                    expressCheckoutElementConfiguration.googlePayVisibility !=
                        ExpressCheckoutElement.Configuration.GooglePayVisibility.Never
                }
                WalletType.Link -> WalletType.Link.takeIf {
                    expressCheckoutElementConfiguration.linkVisibility !=
                        ExpressCheckoutElement.Configuration.LinkVisibility.Never
                }
            }
        }
    }
}
