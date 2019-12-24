package com.stripe.android.model

/**
 * If a card number is tokenized, this is the method that was used. Can be apple_pay or google_pay.
 */
enum class TokenizationMethod(val code: String) {
    ApplePay("apple_pay"),
    GooglePay("google_pay");

    internal companion object {
        internal fun fromCode(code: String?): TokenizationMethod? {
            return values().firstOrNull {
                it.code == code
            }
        }
    }
}
