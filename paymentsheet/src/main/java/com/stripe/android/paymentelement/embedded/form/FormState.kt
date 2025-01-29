package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams

internal data class FormState(
    val code: PaymentMethodCode,
    val primaryButtonIsEnabled: Boolean,
    val primaryButtonLabel: ResolvableString,
    val paymentMethodCreateParams: PaymentMethodCreateParams? = null,
)