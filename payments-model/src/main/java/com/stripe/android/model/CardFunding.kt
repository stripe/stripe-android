package com.stripe.android.model

import androidx.annotation.RestrictTo

enum class CardFunding(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val code: String
) {
    Credit("credit"),
    Debit("debit"),
    Prepaid("prepaid"),
    Unknown("unknown");

    val displayName: String
        get() = when (this) {
            Credit -> "Credit"
            Debit -> "Debit"
            Prepaid -> "Prepaid"
            Unknown -> "Unknown"
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun fromCode(code: String?): CardFunding {
            return entries.firstOrNull { it.code == code?.lowercase() } ?: Unknown
        }
    }
}
