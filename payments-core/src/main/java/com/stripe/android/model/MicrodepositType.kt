package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class MicrodepositType(val value: String) {
    // Two non-unique micro-deposits to the customer's bank account
    AMOUNTS("amounts"),

    // A single micro-deposit sent to the customer's bank account with a unique descriptor code
    DESCRIPTOR_CODE("descriptor_code"),
    UNKNOWN("unknown")
}
