package com.stripe.android.paymentsheet.forms

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class MandateRequirement {
    Always,
    Dynamic,
    Never,
}
