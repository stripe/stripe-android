@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview

internal class FakeAvailableExpressButtonTypesFactory(
    private val availableExpressButtonTypes: List<ExpressButtonType> = listOf(
        ExpressButtonType.GooglePay(
            googlePayConfiguration = GooglePayConfiguration(
                GooglePayConfiguration.Environment.Test,
            ).build(),
        ),
    ),
) : AvailableExpressButtonTypesFactory {

    override fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
        googlePayConfiguration: GooglePayConfiguration.State?,
    ): List<ExpressButtonType> {
        return availableExpressButtonTypes
    }
}
