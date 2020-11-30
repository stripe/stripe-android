package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlin.test.Test

/**
 * Test class for [CountryUtils]
 */
class CountryUtilsTest {

    @Test
    fun `doesCountryUsePostalCode() should return expected result`() {
        assertThat(CountryUtils.doesCountryUsePostalCode("US"))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode("UK"))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode("CA"))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode("DM"))
            .isFalse()
    }

    @Test
    fun getOrderedCountries() {
        assertThat(
            CountryUtils.getOrderedCountries(Locale.getDefault())[0].code
        ).isEqualTo(
            Locale.getDefault().country
        )
    }

    @Test
    fun `getDisplayCountry() should return expected result`() {
        assertThat(CountryUtils.getDisplayCountry("US"))
            .isEqualTo("United States")
        assertThat(CountryUtils.getDisplayCountry("UK"))
            .isEqualTo("UK")
        assertThat(CountryUtils.getDisplayCountry("CA"))
            .isEqualTo("Canada")
        assertThat(CountryUtils.getDisplayCountry("DM"))
            .isEqualTo("Dominica")
    }
}
