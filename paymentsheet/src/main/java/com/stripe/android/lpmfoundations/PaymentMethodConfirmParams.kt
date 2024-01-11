package com.stripe.android.lpmfoundations

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams

/**
 * A wrapper for the details we need to create the payment intent/setup intent confirm request.
 */
internal class PaymentMethodConfirmParams(
    val createParams: PaymentMethodCreateParams,
    val optionsParams: PaymentMethodOptionsParams? = null,
)
