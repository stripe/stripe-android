package com.stripe.android.payments.samsungpay

import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.stripe.android.model.CardBrand

/**
 * Maps Stripe [CardBrand] to Samsung Pay SDK brand constants.
 * Returns null for brands not supported by Samsung Pay.
 */
internal fun CardBrand.toSpaySdkBrand(): SpaySdk.Brand? = when (this) {
    CardBrand.Visa -> SpaySdk.Brand.VISA
    CardBrand.MasterCard -> SpaySdk.Brand.MASTERCARD
    CardBrand.AmericanExpress -> SpaySdk.Brand.AMERICANEXPRESS
    CardBrand.Discover -> SpaySdk.Brand.DISCOVER
    else -> null
}

internal val DEFAULT_SAMSUNG_PAY_BRANDS: Set<CardBrand> = setOf(
    CardBrand.Visa,
    CardBrand.MasterCard,
    CardBrand.AmericanExpress,
    CardBrand.Discover,
)
