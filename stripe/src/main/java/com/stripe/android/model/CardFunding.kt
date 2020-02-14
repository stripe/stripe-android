package com.stripe.android.model

enum class CardFunding(internal val code: String) {
    Credit("credit"),
    Debit("debit"),
    Prepaid("prepaid"),
    Unknown("unknown");

    internal companion object {
        internal fun fromCode(code: String?): CardFunding? {
            return values().firstOrNull { it.code == code }
        }
    }
}
