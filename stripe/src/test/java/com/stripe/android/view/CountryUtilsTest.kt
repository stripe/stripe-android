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
            Locale.getDefault().getCountryCode()
        )
    }

    @Test
    fun `getDisplayCountry() should return expected result`() {
        assertThat(CountryUtils.getDisplayCountry(CountryCode.US))
            .isEqualTo("United States")
        assertThat(CountryUtils.getDisplayCountry(CountryCode.GB))
            .isEqualTo("United Kingdom")
        assertThat(CountryUtils.getDisplayCountry(CountryCode.CA))
            .isEqualTo("Canada")
        assertThat(CountryUtils.getDisplayCountry(CountryCode("DM")))
            .isEqualTo("Dominica")
        assertThat(CountryUtils.getDisplayCountry(CountryCode("DMd")))
            .isEqualTo("DMD")
    }
}
