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
        assertThat(CountryUtils.doesCountryUsePostalCode(CountryCode("US")))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode(CountryCode("UK")))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode(CountryCode("CA")))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode(CountryCode("DM")))
            .isFalse()
    }

    @Test
    fun getOrderedCountries() {
        assertThat(
            CountryUtils.getOrderedCountries(Locale.getDefault())[0].code.twoLetters
        ).isEqualTo(
            Locale.getDefault().country
        )
    }

    @Test
    fun `getDisplayCountry() should return expected result`() {
        var currentLocale = Locale.US
        assertThat(CountryUtils.getDisplayCountry(CountryCode("US"), currentLocale))
            .isEqualTo("United States")
        assertThat(CountryUtils.getDisplayCountry(CountryCode("UK"), currentLocale))
            .isEqualTo("UK")
        assertThat(CountryUtils.getDisplayCountry(CountryCode("CA"), currentLocale))
            .isEqualTo("Canada")
        assertThat(CountryUtils.getDisplayCountry(CountryCode("DM"), currentLocale))
            .isEqualTo("Dominica")

        currentLocale = Locale("de", "DE")
        assertThat(CountryUtils.getDisplayCountry(CountryCode("DE"), currentLocale))
            .isEqualTo("Deutschland")
    }

    @Test
    fun `getOrderedCountriesLocaleLanguage() in the language of the current locale`() {
        val currentLocale = Locale("de", "DE")
        val germany = CountryUtils.getOrderedCountries(currentLocale)
            .firstOrNull()

        // If the current locale is germany it should be first in the list, and the german
        // word for germany
        assertThat(germany?.name)
            .isEqualTo("Deutschland")
    }

    @Test
    fun `getCountryByName() in the language of current locale`() {
    }
}
