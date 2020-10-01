package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentMethodCreateParams

internal sealed class Selection {
    object GooglePay : Selection()
    data class Saved(val paymentMethodId: String) : Selection()
    data class New(val paymentMethodCreateParams: PaymentMethodCreateParams) : Selection()
}
