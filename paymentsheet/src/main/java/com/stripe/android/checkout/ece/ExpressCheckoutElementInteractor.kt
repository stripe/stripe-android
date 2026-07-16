@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.ece

import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.link.account.LinkAccountHolder
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
        stateHolder.stateFlow,
    ) { linkAccountInfo, state ->
        if (state == null) {
            return@combineAsStateFlow ExpressCheckoutElementInteractor.State(expressButtons = emptyList())
        }

        ExpressCheckoutElementInteractor.State(
            expressButtons = state.asCheckoutSession().availableExpressButtonTypes.map { walletType ->
                when (walletType) {
                    WalletType.Link -> ExpressButton.Link.create(
                        paymentMethodMetadata = state.paymentMethodMetadata,
                        linkAccountInfo = linkAccountInfo,
                    )
                    WalletType.GooglePay -> ExpressButton.GooglePay.create(
                        paymentMethodMetadata = state.paymentMethodMetadata,
                    )
                }
            },
        )
    }
}
