package com.stripe.android.paymentelement.embedded.linkinline

import app.cash.turbine.Turbine
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.LinkBrand

internal class FakeEmbeddedLinkInlineActivityHost : EmbeddedLinkInlineActivityHost {
    val launchLinkWalletCalls = Turbine<LaunchLinkWalletCall>()
    val payWithLinkWalletCalls = Turbine<LinkBrand>()

    override fun launchLinkWallet(
        configuration: LinkConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountInfo: LinkAccountUpdate.Value,
        selectedPayment: LinkPaymentMethod?,
    ) {
        launchLinkWalletCalls.add(
            LaunchLinkWalletCall(configuration, paymentMethodMetadata, linkAccountInfo, selectedPayment)
        )
    }

    override fun payWithLinkWallet(linkBrand: LinkBrand) {
        payWithLinkWalletCalls.add(linkBrand)
    }

    fun ensureAllEventsConsumed() {
        launchLinkWalletCalls.ensureAllEventsConsumed()
        payWithLinkWalletCalls.ensureAllEventsConsumed()
    }

    data class LaunchLinkWalletCall(
        val configuration: LinkConfiguration,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val linkAccountInfo: LinkAccountUpdate.Value,
        val selectedPayment: LinkPaymentMethod?,
    )
}
