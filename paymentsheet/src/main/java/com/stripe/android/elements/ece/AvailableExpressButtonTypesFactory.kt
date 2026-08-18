@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

internal fun interface AvailableExpressButtonTypesFactory {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
    ): List<ExpressButtonType>
}

internal class DefaultAvailableExpressButtonTypesFactory @Inject internal constructor() :
    AvailableExpressButtonTypesFactory {
    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
    ): List<ExpressButtonType> {
        return paymentMethodMetadata.availableWallets.mapNotNull { walletType ->
            when (walletType) {
                WalletType.GooglePay -> ExpressButtonType.GooglePay(
                        googlePayConfiguration = expressCheckoutElementConfiguration.googlePayConfiguration,
                    ).takeIf {
                    expressCheckoutElementConfiguration.googlePayConfiguration.display ==
                        ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Automatic
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
