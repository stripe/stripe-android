@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

internal fun interface AvailableExpressButtonTypesFactory {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
    ): List<WalletType>
}

internal class DefaultAvailableExpressButtonTypesFactory @Inject internal constructor() :
    AvailableExpressButtonTypesFactory {
    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State
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