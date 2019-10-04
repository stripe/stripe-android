package com.stripe.android.view

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShippingPostalCodeValidatorTest {

    @Test
    fun testPostalCodeOptional() {
        assertTrue(VALIDATOR.isValid("", "US",
            listOf(ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD),
            emptyList()))
    }

    @Test
    fun testCountryCodeOptional() {
        assertTrue(VALIDATOR.isValid("94107", "",
            listOf(ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD),
            emptyList()))
    }

    @Test
    fun usZipCodeTest() {
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
    fun canadianPostalCodeTest() {
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
    fun ukPostalCodeTest() {
        assertTrue(isValid("L1 8JQ", "GB"))
        assertTrue(isValid("GU16 7HF", "GB"))
        assertTrue(isValid("PO16 7GZ", "GB"))
        assertFalse(isValid("94107", "GB"))
        assertFalse(isValid("94107-1234", "GB"))
        assertFalse(isValid("!1A 0B1", "GB"))
        assertFalse(isValid("Z1A 0B1", "GB"))
        assertFalse(isValid("123", "GB"))
    }

    private fun isValid(input: String, countryCode: String): Boolean {
        return VALIDATOR.isValid(input, countryCode,
            emptyList(), emptyList())
    }

    companion object {
        private val VALIDATOR = ShippingPostalCodeValidator()
    }
}
