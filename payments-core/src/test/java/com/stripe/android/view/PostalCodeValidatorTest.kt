package com.stripe.android.view

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostalCodeValidatorTest {

    @Test
    fun testPostalCodeOptional() {
        assertTrue(
            VALIDATOR.isValid(
                postalCode = "",
                countryCode = "US",
                optionalShippingInfoFields = listOf(
                    ShippingInfoWidget.CustomizableShippingField.PostalCode
                ),
                hiddenShippingInfoFields = emptyList()
            )
        )
    }

    @Test
    fun testCountryCodeOptional() {
        assertTrue(
            VALIDATOR.isValid(
                postalCode = "94107",
                countryCode = "",
                optionalShippingInfoFields = listOf(
                    ShippingInfoWidget.CustomizableShippingField.PostalCode
                ),
                hiddenShippingInfoFields = emptyList()
            )
        )
    }

    @Test
    fun testUsZipCode() {
        assertTrue(isValid("94107", "US"))
        assertTrue(isValid("94107-1234", "US"))
        assertFalse(isValid("941071234", "US"))
        assertFalse(isValid("9410a1234", "US"))
        assertFalse(isValid("94107-", "US"))
        assertFalse(isValid("9410&", "US"))
        assertFalse(isValid("K1A 0B1", "US"))
        assertFalse(isValid("", "US"))
    }

    @Test
    fun testUkPostalCode() {
        assertTrue(isValid("L1 8JQ", "GB"))
        assertTrue(isValid("GU16 7HF", "GB"))
        assertTrue(isValid("PO16 7GZ", "GB"))
        assertFalse(isValid("", "GB"))
        assertFalse(isValid("    ", "GB"))
    }

    @Test
    fun testCountryWithoutPostalCode() {
        assertTrue(isValid("", "DE"))
        assertTrue(isValid("ABC123", "DE"))
    }

    private fun isValid(postalCode: String, countryCode: String): Boolean {
        return VALIDATOR.isValid(postalCode, countryCode, emptyList(), emptyList())
    }

    private companion object {
        private val VALIDATOR = PostalCodeValidator()
    }
}
