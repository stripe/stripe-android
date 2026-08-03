@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.ece

import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview

internal sealed interface ExpressButtonType {
    val paymentMethodType: ExpressCheckoutElement.PaymentMethod

    data object Link : ExpressButtonType {
        override val paymentMethodType: ExpressCheckoutElement.PaymentMethod = ExpressCheckoutElement.PaymentMethod.Link
    }

    data class GooglePay(
        val googlePayConfiguration: GooglePayConfiguration.State,
    ) : ExpressButtonType {
        override val paymentMethodType: ExpressCheckoutElement.PaymentMethod = ExpressCheckoutElement.PaymentMethod.GooglePay
    }
}
