package com.stripe.android.paymentsheet.model

import com.stripe.android.core.strings.ResolvableString

internal data class MandateText(
    val text: ResolvableString?,
    val showAbovePrimaryButton: Boolean
)
