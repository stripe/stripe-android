package com.stripe.android.view

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostalCodeValidatorTest {

    @Test
    fun testPostalCodeOptional() {
        assertTrue(VALIDATOR.isValid(
            postalCode = "",
            countryCode = "US",
            optionalShippingInfoFields = listOf(
                ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD
            ),
            hiddenShippingInfoFields = emptyList()
        ))
    }

    @Test
    fun testCountryCodeOptional() {
        assertTrue(VALIDATOR.isValid(
            postalCode = "94107",
            countryCode = "",
            optionalShippingInfoFields = listOf(
                ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD
            ),
            hiddenShippingInfoFields = emptyList()
        ))
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
    fun testCanadianPostalCode() {
        assertTrue(isValid("K1A 0B1", "CA"))
        assertTrue(isValid("B1Z 0B9", "CA"))
        assertFalse(isValid("K1A 0D1", "CA"))
        assertFalse(isValid("94107", "CA"))
        assertFalse(isValid("94107-1234", "CA"))
        assertFalse(isValid("W1A 0B1", "CA"))
        assertFalse(isValid("123", "CA"))
        assertFalse(isValid("", "CA"))
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
        assertTrue(isValid("", CountryUtils.NO_POSTAL_CODE_COUNTRIES.first()))
        assertTrue(isValid("ABC123", CountryUtils.NO_POSTAL_CODE_COUNTRIES.first()))
    }

    private fun isValid(postalCode: String, countryCode: String): Boolean {
        return VALIDATOR.isValid(postalCode, countryCode, emptyList(), emptyList())
    }

    private companion object {
        private val VALIDATOR = PostalCodeValidator()
    }
}
