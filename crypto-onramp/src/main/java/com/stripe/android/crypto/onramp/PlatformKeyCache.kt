package com.stripe.android.crypto.onramp

/** Caches a platform key for the customer, country hint and merchant that resolved it. */
internal data class PlatformKeyCache(
    val publishableKey: String,
    val cryptoCustomerId: String?,
    val countryHint: String?,
    val merchantPublishableKey: String?,
) {
    fun matches(
        cryptoCustomerId: String?,
        countryHint: String?,
        merchantPublishableKey: String?,
    ): Boolean = this.cryptoCustomerId == cryptoCustomerId && this.countryHint == countryHint &&
        this.merchantPublishableKey == merchantPublishableKey
}
