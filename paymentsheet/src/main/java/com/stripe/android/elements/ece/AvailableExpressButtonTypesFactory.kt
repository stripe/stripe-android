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
                    paymentMethodMetadata.shouldShowLinkButton &&
                        !requiresShippingAddress
                }
            }
        }

        val buttonLayout = expressCheckoutElementConfiguration.appearance.buttonLayout
        val maxColumns = buttonLayout.maxColumns ?: return availableExpressButtonTypes
        val maxRows = buttonLayout.maxRows ?: return availableExpressButtonTypes
        val visibleButtonCount = maxColumns * maxRows

        if (visibleButtonCount >= availableExpressButtonTypes.size) {
            return availableExpressButtonTypes
        }

        return if (visibleButtonCount == 1) {
            listOfNotNull(
                availableExpressButtonTypes.firstOrNull { it is ExpressButtonType.GooglePay }
                    ?: availableExpressButtonTypes.firstOrNull()
            )
        } else {
            availableExpressButtonTypes.take(visibleButtonCount)
        }
    }
}
