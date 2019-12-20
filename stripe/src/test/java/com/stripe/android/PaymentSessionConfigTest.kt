package com.stripe.android

import com.stripe.android.PaymentSessionFixtures.CONFIG
import com.stripe.android.model.ShippingInformation
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
        assertEquals(CONFIG, ParcelUtils.create(CONFIG))
    }

    @Test
    fun create_withValidCountryCode_succeeds() {
        val allowedShippingCountryCodes = setOf("us", "CA")
        val config = CONFIG.copy(
            allowedShippingCountryCodes = allowedShippingCountryCodes
        )
        assertEquals(allowedShippingCountryCodes, config.allowedShippingCountryCodes)
    }

    @Test
    fun create_withEmptyCountryCodesList_succeeds() {
        val config = CONFIG.copy(
            allowedShippingCountryCodes = emptySet()
        )
        assertTrue(config.allowedShippingCountryCodes.isEmpty())
    }

    @Test
    fun create_withInvalidCountryCode_throwsException() {
        val exception: IllegalArgumentException = assertFailsWith {
            CONFIG.copy(
                allowedShippingCountryCodes = setOf("invalid_country_code")
            )
        }
        assertEquals(
            "'invalid_country_code' is not a valid country code",
            exception.message
        )
    }

    @Test
    fun create_withShippingMethodsRequiredAndShippingInformationValidatorProvided_withoutShippingMethodsFactory_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            PaymentSessionConfig.Builder()
                .setShippingInfoRequired(true)
                .setShippingMethodsRequired(true)
                .setShippingInformationValidator(FakeShippingInfoValidator())
                .build()
        }
    }

    private class FakeShippingInfoValidator : PaymentSessionConfig.ShippingInformationValidator {
        override fun isValid(shippingInformation: ShippingInformation): Boolean {
            return true
        }

        override fun getErrorMessage(shippingInformation: ShippingInformation): String {
            return ""
        }
    }
}
