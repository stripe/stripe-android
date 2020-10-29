package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentMethodCreateParams

internal sealed class PaymentSelection {
    object GooglePay : PaymentSelection()

    data class Saved(
        val paymentMethodId: String
    ) : PaymentSelection()

    data class New(
        val paymentMethodCreateParams: PaymentMethodCreateParams
    ) : PaymentSelection()
}
