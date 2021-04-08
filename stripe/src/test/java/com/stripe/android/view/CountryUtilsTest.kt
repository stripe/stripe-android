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
        var currentLocale = Locale.US
        assertThat(CountryUtils.getDisplayCountry("US", currentLocale))
            .isEqualTo("United States")
        assertThat(CountryUtils.getDisplayCountry("UK", currentLocale))
            .isEqualTo("UK")
        assertThat(CountryUtils.getDisplayCountry("CA", currentLocale))
            .isEqualTo("Canada")
        assertThat(CountryUtils.getDisplayCountry("DM", currentLocale))
            .isEqualTo("Dominica")

        currentLocale = Locale("de", "DE")
        assertThat(CountryUtils.getDisplayCountry("DE", currentLocale))
            .isEqualTo("Deutschland")
    }

    @Test
    fun `getOrderedCountriesLocaleLanguage() in the language of the current locale`() {
        val currentLocale = Locale("de", "DE")
        val germany = CountryUtils.getOrderedCountries(currentLocale)
            .firstOrNull()
        val secondCountry = CountryUtils.getOrderedCountries(currentLocale)
            .firstOrNull()

        // If the current locale is germany it should be first in the list, and the german
        // word for germany
        assertThat(germany?.name)
            .isEqualTo("Deutschland")
        assertThat(secondCountry?.name)
            .isEqualTo("Deutschland")
    }

    @Test
    fun `getCountryByName() in the language of current locale`() {
    }
}
