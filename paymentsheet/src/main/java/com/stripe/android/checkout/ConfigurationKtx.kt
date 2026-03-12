package com.stripe.android.checkout

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
internal fun PaymentSheet.Configuration.forCheckoutSession(
    state: InternalState,
): PaymentSheet.Configuration {
    val response = state.checkoutSessionResponse
    val shouldSetEmail = defaultBillingDetails?.email == null && response.customerEmail != null
    val shouldSetName = shippingDetails?.name == null && state.shippingName != null

    if (!shouldSetEmail && !shouldSetName) return this

    return newBuilder().apply {
        if (shouldSetEmail) {
            defaultBillingDetails(
                PaymentSheet.BillingDetails(
                    address = defaultBillingDetails?.address,
                    email = response.customerEmail,
                    name = defaultBillingDetails?.name,
                    phone = defaultBillingDetails?.phone,
                )
            )
        }
        if (shouldSetName) {
            shippingDetails(
                AddressDetails(
                    name = state.shippingName,
                    address = shippingDetails?.address,
                    phoneNumber = shippingDetails?.phoneNumber,
                    isCheckboxSelected = shippingDetails?.isCheckboxSelected,
                )
            )
        }
    }.build()
}
