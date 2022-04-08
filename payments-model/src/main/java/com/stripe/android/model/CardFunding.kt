package com.stripe.android.model

import androidx.annotation.RestrictTo

enum class CardFunding(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val code: String
) {
    Credit("credit"),
    Debit("debit"),
    Prepaid("prepaid"),
    Unknown("unknown");

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun fromCode(code: String?): CardFunding? {
            return values().firstOrNull { it.code == code }
        }
    }
}
