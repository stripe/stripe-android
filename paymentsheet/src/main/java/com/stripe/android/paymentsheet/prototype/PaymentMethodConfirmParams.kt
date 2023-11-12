package com.stripe.android.paymentsheet.prototype

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams

internal class PaymentMethodConfirmParams(
    val createParams: PaymentMethodCreateParams,
    val optionsParams: PaymentMethodOptionsParams? = null,
)
