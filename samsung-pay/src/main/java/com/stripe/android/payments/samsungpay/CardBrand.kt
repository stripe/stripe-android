package com.stripe.android.payments.samsungpay

import com.samsung.android.sdk.samsungpay.v2.SpaySdk

/**
 * Card brands supported by Samsung Pay.
 *
 * This wraps Samsung Pay SDK brand constants so the public API
 * doesn't leak Samsung SDK types.
 */
enum class CardBrand {
    Visa,
    Mastercard,
    AmericanExpress,
    Discover;

    internal fun toSpaySdkBrand(): SpaySdk.Brand = when (this) {
        Visa -> SpaySdk.Brand.VISA
        Mastercard -> SpaySdk.Brand.MASTERCARD
        AmericanExpress -> SpaySdk.Brand.AMERICANEXPRESS
        Discover -> SpaySdk.Brand.DISCOVER
    }

    companion object {
        val DEFAULT_BRANDS: Set<CardBrand> = setOf(
            Visa,
            Mastercard,
            AmericanExpress,
            Discover,
        )
    }
}
