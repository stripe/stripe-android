@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

internal fun interface AvailableExpressButtonTypesFactory {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
        googlePayConfiguration: GooglePayConfiguration.State?,
    ): List<ExpressButtonType>
}

internal class DefaultAvailableExpressButtonTypesFactory @Inject internal constructor() :
    AvailableExpressButtonTypesFactory {
    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
        googlePayConfiguration: GooglePayConfiguration.State?,
    ): List<ExpressButtonType> {
        return paymentMethodMetadata.availableWallets.mapNotNull { walletType ->
            when (walletType) {
                WalletType.GooglePay -> googlePayConfiguration?.let {
                    ExpressButtonType.GooglePay(
                        googlePayConfiguration = it,
                    ).takeIf {
                        expressCheckoutElementConfiguration.googlePayVisibility !=
                            ExpressCheckoutElement.Configuration.GooglePayVisibility.Never
                    }
                }
                WalletType.Link -> ExpressButtonType.Link.takeIf {
                    expressCheckoutElementConfiguration.linkVisibility !=
                        ExpressCheckoutElement.Configuration.LinkVisibility.Never &&
                        !expressCheckoutElementConfiguration.shippingAddressRequired
                }
            }
        }
    }
}
