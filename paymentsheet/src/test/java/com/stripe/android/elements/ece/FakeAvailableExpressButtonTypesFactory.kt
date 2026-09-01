@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview

internal class FakeAvailableExpressButtonTypesFactory(
    private val availableExpressButtonTypes: List<ExpressButtonType> = listOf(
        ExpressButtonType.GooglePay(
            googlePayConfiguration = GooglePayConfiguration().build(),
        ),
    ),
) : AvailableExpressButtonTypesFactory {

    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State?,
    ): List<ExpressButtonType> {
        return availableExpressButtonTypes
    }
}
