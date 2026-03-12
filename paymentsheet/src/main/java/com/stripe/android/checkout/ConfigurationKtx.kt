package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentSheet.Configuration.forCheckoutSession(
    state: InternalState,
): PaymentSheet.Configuration {
    val response = state.checkoutSessionResponse

    return newBuilder().apply {
        defaultBillingDetails(
            PaymentSheet.BillingDetails(
                address = defaultBillingDetails?.address,
                email = defaultBillingDetails?.email ?: response.customerEmail,
                name = defaultBillingDetails?.name ?: state.billingName,
                phone = defaultBillingDetails?.phone,
            )
        )
        shippingDetails(
            AddressDetails(
                name = shippingDetails?.name ?: state.shippingName,
                address = shippingDetails?.address,
                phoneNumber = shippingDetails?.phoneNumber,
                isCheckboxSelected = shippingDetails?.isCheckboxSelected,
            )
        )
    }.build()
}
