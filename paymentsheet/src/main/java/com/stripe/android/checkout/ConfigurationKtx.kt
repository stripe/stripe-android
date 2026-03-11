package com.stripe.android.checkout

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal fun PaymentSheet.Configuration.forCheckoutSession(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.Configuration {
    if (defaultBillingDetails?.email != null) return this
    val customerEmail = checkoutSessionResponse.customerEmail ?: return this
    return newBuilder()
        .defaultBillingDetails(
            PaymentSheet.BillingDetails(
                address = defaultBillingDetails?.address,
                email = customerEmail,
                name = defaultBillingDetails?.name,
                phone = defaultBillingDetails?.phone,
            )
        )
        .build()
}
