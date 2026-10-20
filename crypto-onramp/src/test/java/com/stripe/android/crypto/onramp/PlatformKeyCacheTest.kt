package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class PlatformKeyCacheTest {
    private val cache = PlatformKeyCache(
        publishableKey = "pk_platform",
        cryptoCustomerId = "crc_customer",
        countryHint = "GB",
        merchantPublishableKey = "pk_merchant",
    )

    @Test
    fun `same routing inputs reuse the cache`() {
        assertThat(cache.matches("crc_customer", "GB", "pk_merchant")).isTrue()
    }

    @Test
    fun `different customer invalidates the cache`() {
        assertThat(cache.matches("crc_other", "GB", "pk_merchant")).isFalse()
    }

    @Test
    fun `different hint invalidates the cache`() {
        assertThat(cache.matches("crc_customer", "US", "pk_merchant")).isFalse()
    }

    @Test
    fun `removing hint invalidates the cache`() {
        assertThat(cache.matches("crc_customer", null, "pk_merchant")).isFalse()
    }

    @Test
    fun `different merchant invalidates the cache`() {
        assertThat(cache.matches("crc_customer", "GB", "pk_other")).isFalse()
    }

    @Test
    fun `customer arrival invalidates the pre-auth cache`() {
        val preAuthCache = cache.copy(cryptoCustomerId = null)

        assertThat(preAuthCache.matches(null, "GB", "pk_merchant")).isTrue()
        assertThat(preAuthCache.matches("crc_customer", "GB", "pk_merchant")).isFalse()
    }
}
