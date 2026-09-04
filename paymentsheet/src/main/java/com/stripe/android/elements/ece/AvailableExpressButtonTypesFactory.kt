@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import com.stripe.android.elements.CheckoutGooglePayConfiguration
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

internal fun interface AvailableExpressButtonTypesFactory {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata?,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State?,
        requiresShippingAddress: Boolean,
    ): List<ExpressButtonType>
}

internal class DefaultAvailableExpressButtonTypesFactory @Inject internal constructor() :
    AvailableExpressButtonTypesFactory {
    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata?,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State?,
        requiresShippingAddress: Boolean,
    ): List<ExpressButtonType> {
        expressCheckoutElementConfiguration ?: return emptyList()
        paymentMethodMetadata ?: return emptyList()

        val availableExpressButtonTypes = paymentMethodMetadata.availableWallets.mapNotNull { walletType ->
            when (walletType) {
                WalletType.GooglePay -> ExpressButtonType.GooglePay(
                        googlePayConfiguration = expressCheckoutElementConfiguration.googlePayConfiguration,
                    ).takeIf {
                    expressCheckoutElementConfiguration.googlePayConfiguration.display ==
                        CheckoutGooglePayConfiguration.Display.Automatic
                }
                WalletType.Link -> ExpressButtonType.Link.takeIf {
                    expressCheckoutElementConfiguration.linkConfiguration.display ==
                        ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic &&
                        !requiresShippingAddress
                }
            }
        }

        val paymentMethodOrder = expressCheckoutElementConfiguration.paymentMethodOrder
        if (paymentMethodOrder.isEmpty()) {
            return availableExpressButtonTypes
        }

        return availableExpressButtonTypes.sortedBy { expressButtonType ->
            paymentMethodOrder.indexOfFirst { paymentMethod ->
                paymentMethod.matches(expressButtonType)
            }.takeIf { it >= 0 } ?: Int.MAX_VALUE
        }
    }

    private fun ExpressCheckoutElement.Configuration.PaymentMethodType.matches(
        expressButtonType: ExpressButtonType,
    ): Boolean {
        return when (this) {
            ExpressCheckoutElement.Configuration.PaymentMethodType.GooglePay ->
                expressButtonType is ExpressButtonType.GooglePay
            ExpressCheckoutElement.Configuration.PaymentMethodType.Link -> expressButtonType is ExpressButtonType.Link
        }
    }
}
