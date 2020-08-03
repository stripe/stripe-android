package com.stripe.android.model

/**
 * If a card number is tokenized, this is the method that was used.
 *
 * See [tokenization_method](https://stripe.com/docs/api/cards/object#card_object-tokenization_method)
 */
enum class TokenizationMethod(val code: String) {
    ApplePay("apple_pay"),
    GooglePay("google_pay"),
    Masterpass("masterpass"),
    VisaCheckout("visa_checkout");

    internal companion object {
        internal fun fromCode(code: String?): TokenizationMethod? {
            return values().firstOrNull {
                it.code == code
            }
        }
    }
}
