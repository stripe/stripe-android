package com.stripe.android.checkout

import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun Address.State.asPaymentSheet(): PaymentSheet.Address = PaymentSheet.Address(
    city = city,
    country = country,
    line1 = line1,
    line2 = line2,
    postalCode = postalCode,
    state = state,
)
