package com.stripe.android.checkout

import com.stripe.android.model.Address
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Address.State.asPaymentSheet(): PaymentSheet.Address = PaymentSheet.Address(
    city = city,
    country = country,
    line1 = line1,
    line2 = line2,
    postalCode = postalCode,
    state = state,
)

@OptIn(CheckoutSessionPreview::class)
internal fun Address.toCheckoutAddress(): CheckoutController.Address.State? {
    return CheckoutController.Address.State(
        city = city,
        country = country ?: return null,
        line1 = line1,
        line2 = line2,
        postalCode = postalCode,
        state = state,
    )
}

@OptIn(CheckoutSessionPreview::class)
internal fun AddressDetails.toCheckoutAddress(): CheckoutController.Address.State? {
    val address = address ?: return null
    return CheckoutController.Address.State(
        city = address.city,
        country = address.country ?: return null,
        line1 = address.line1,
        line2 = address.line2,
        postalCode = address.postalCode,
        state = address.state,
    )
}
