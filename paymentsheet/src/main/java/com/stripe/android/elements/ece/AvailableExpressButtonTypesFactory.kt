@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import androidx.annotation.VisibleForTesting
import com.stripe.android.elements.CheckoutGooglePayConfiguration
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject
import kotlin.math.min

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
        val visibleButtonCount = calculateVisibleButtonCount(
            buttonCount = availableExpressButtonTypes.size,
            maxColumns = buttonLayout.maxColumns,
            maxRows = buttonLayout.maxRows,
        )

        if (visibleButtonCount >= availableExpressButtonTypes.size) {
            return availableExpressButtonTypes
        }

        val googlePayButton = availableExpressButtonTypes.find { it is ExpressButtonType.GooglePay }
        val naivelyTruncatedAvailableButtons = availableExpressButtonTypes.take(visibleButtonCount)
        return if (googlePayButton != null && !naivelyTruncatedAvailableButtons.contains(googlePayButton)) {
            naivelyTruncatedAvailableButtons.subList(0, visibleButtonCount - 1).plus(googlePayButton)
        } else {
            naivelyTruncatedAvailableButtons
        }
    }
}

@VisibleForTesting
internal fun calculateVisibleButtonCount(
    buttonCount: Int,
    maxColumns: Int?,
    maxRows: Int?,
): Int {
    if (maxColumns == null || maxRows == null) {
        return buttonCount
    }

    val displayableInGrid = maxRows * maxColumns

    return min(
        displayableInGrid,
        buttonCount,
    )
}
