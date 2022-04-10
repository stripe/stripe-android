package com.stripe.android.model

import androidx.annotation.RestrictTo

/**
 * If a card number is tokenized, this is the method that was used.
 *
 * See [tokenization_method](https://stripe.com/docs/api/cards/object#card_object-tokenization_method)
 */
enum class TokenizationMethod(
    private val code: Set<String>
) {
    ApplePay(
        setOf("apple_pay")
    ),
    GooglePay(
        setOf("android_pay", "google")
    ),
    Masterpass(
        setOf("masterpass")
    ),
    VisaCheckout(
        setOf("visa_checkout")
    );

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun fromCode(code: String?): TokenizationMethod? {
            return values().firstOrNull {
                it.code.contains(code)
            }
        }
    }
}
