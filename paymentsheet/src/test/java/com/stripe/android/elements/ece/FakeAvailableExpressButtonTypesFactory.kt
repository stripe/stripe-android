@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import com.stripe.android.checkout.CheckoutSessionPreview
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal class FakeAvailableExpressButtonTypesFactory(
    private val availableExpressButtonTypes: List<ExpressButtonType> = listOf(
        ExpressButtonType.GooglePay(
            googlePayConfiguration = GooglePayConfiguration().build(),
        ),
    ),
) : AvailableExpressButtonTypesFactory {

    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata?,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State?,
        requiresShippingAddress: Boolean,
    ): List<ExpressButtonType> {
        return availableExpressButtonTypes
    }
}
