package com.stripe.android.view

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for [CountryUtils]
 */
class CountryUtilsTest {

    @Test
    fun postalCodeCountryTest() {
        assertTrue(CountryUtils.doesCountryUsePostalCode("US"))
        assertTrue(CountryUtils.doesCountryUsePostalCode("UK"))
        assertTrue(CountryUtils.doesCountryUsePostalCode("CA"))
        assertFalse(CountryUtils.doesCountryUsePostalCode("DM"))
    }

    @Test
    fun getOrderedCountries() {
        assertEquals(
            Locale.getDefault().displayCountry,
            CountryUtils.getOrderedCountries(Locale.getDefault())[0]
        )
    }
}
