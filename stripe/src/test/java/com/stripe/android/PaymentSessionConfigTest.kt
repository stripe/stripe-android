package com.stripe.android

import com.stripe.android.PaymentSessionFixtures.PAYMENT_SESSION_CONFIG
import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSessionConfigTest {

    @Test
    fun testParcel() {
        assertEquals(PAYMENT_SESSION_CONFIG, ParcelUtils.create(PAYMENT_SESSION_CONFIG))
        assertEquals(
            PaymentSessionFixtures.PAYMENT_SESSION_CONFIG,
            ParcelUtils.create(PaymentSessionFixtures.PAYMENT_SESSION_CONFIG)
        )
    }

    @Test
    fun create_withValidCountryCode_succeeds() {
        val allowedShippingCountryCodes = setOf("us", "CA")
        val config = PaymentSessionFixtures.PAYMENT_SESSION_CONFIG.copy(
            allowedShippingCountryCodes = allowedShippingCountryCodes
        )
        assertEquals(allowedShippingCountryCodes, config.allowedShippingCountryCodes)
    }

    @Test
    fun create_withEmptyCountryCodesList_succeeds() {
        val config = PaymentSessionFixtures.PAYMENT_SESSION_CONFIG.copy(
            allowedShippingCountryCodes = emptySet()
        )
        assertTrue(config.allowedShippingCountryCodes.isEmpty())
    }

    @Test
    fun create_withInvalidCountryCode_throwsException() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PaymentSessionFixtures.PAYMENT_SESSION_CONFIG.copy(
                allowedShippingCountryCodes = setOf("invalid_country_code")
            )
        }
        assertEquals(
            "'invalid_country_code' is not a valid country code",
            exception.message
        )
    }
}
