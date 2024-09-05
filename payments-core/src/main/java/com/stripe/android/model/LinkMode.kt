package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LinkMode(val value: String) {
    Passthrough("passthrough"),
    PaymentMethod("payment_method_mode"),
}
