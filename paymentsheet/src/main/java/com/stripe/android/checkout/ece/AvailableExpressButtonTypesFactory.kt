@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.checkout.GooglePayConfiguration
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
                        expressCheckoutElementConfiguration.paymentMethods.googlePay !=
                            ExpressCheckoutElement.Configuration.PaymentMethods.GooglePayVisibility.Never
                    }
                }
                WalletType.Link -> ExpressButtonType.Link.takeIf {
                    expressCheckoutElementConfiguration.paymentMethods.link !=
                        ExpressCheckoutElement.Configuration.PaymentMethods.LinkVisibility.Never
                }
            }
        }
    }
}
