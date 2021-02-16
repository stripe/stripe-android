package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentSessionFixtures.CONFIG
import com.stripe.android.model.ShippingInformation
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class PaymentSessionConfigTest {

    @Test
    fun testParcel() {
        assertThat(ParcelUtils.create(CONFIG)).isEqualTo(CONFIG)
    }

    @Test
    fun create_withValidCountryCode_succeeds() {
        val allowedShippingCountryCodes = setOf("us", "CA")
        val config = CONFIG.copy(
            allowedShippingCountryCodes = allowedShippingCountryCodes
        )
        assertThat(config.allowedShippingCountryCodes).isEqualTo(allowedShippingCountryCodes)
    }

    @Test
    fun create_withEmptyCountryCodesList_succeeds() {
        val config = CONFIG.copy(
            allowedShippingCountryCodes = emptySet()
        )
        assertThat(config.allowedShippingCountryCodes.isEmpty()).isTrue()
    }

    @Test
    fun create_withInvalidCountryCode_throwsException() {
        val exception: IllegalArgumentException = assertFailsWith {
            CONFIG.copy(
                allowedShippingCountryCodes = setOf("invalid_country_code")
            )
        }
        assertThat(exception.message).isEqualTo("'invalid_country_code' is not a valid country code")
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
