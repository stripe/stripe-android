package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection

internal data class FormState(
    val code: PaymentMethodCode,
    val primaryButtonIsEnabled: Boolean,
    val primaryButtonLabel: ResolvableString,
    val paymentSelection: PaymentSelection? = null
)