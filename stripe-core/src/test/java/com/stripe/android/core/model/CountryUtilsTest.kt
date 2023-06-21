package com.stripe.android.core.model

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test

/**
 * Test class for [CountryUtils]
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CountryUtilsTest {

    @Test
    fun `doesCountryUsePostalCode() should return expected result`() {
        assertThat(CountryUtils.doesCountryUsePostalCode(CountryCode.create("US")))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode(CountryCode.create("GB")))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode(CountryCode.create("CA")))
            .isTrue()
        assertThat(CountryUtils.doesCountryUsePostalCode(CountryCode.create("DM")))
            .isFalse()
    }

    @Test
    fun getOrderedCountries() {
        val defaultLocaleSecondCountryName = CountryUtils.getOrderedCountries(Locale.getDefault())[1].name
        assertThat(
            CountryUtils.getOrderedCountries(Locale.getDefault())[0].code
        ).isEqualTo(
            Locale.getDefault().getCountryCode()
        )

        // Make sure caching updates the localized country list.  We look at index
        // 1 because the 0 is the country of the current locale.
        assertThat(
            CountryUtils.getOrderedCountries(Locale.CHINESE)[1].name
        ).isNotEqualTo(
            defaultLocaleSecondCountryName
        )
    }

    @Test
    fun `getDisplayCountry() should return expected result`() {
        var currentLocale = Locale.US
        assertThat(CountryUtils.getDisplayCountry(CountryCode.US, currentLocale))
            .isEqualTo("United States")
        assertThat(CountryUtils.getDisplayCountry(CountryCode.GB, currentLocale))
            .isEqualTo("United Kingdom")
        assertThat(CountryUtils.getDisplayCountry(CountryCode.CA, currentLocale))
            .isEqualTo("Canada")
        assertThat(CountryUtils.getDisplayCountry(CountryCode.create("DM"), currentLocale))
            .isEqualTo("Dominica")
        assertThat(CountryUtils.getDisplayCountry(CountryCode.create("DMd"), currentLocale))
            .isEqualTo("DMD")

        currentLocale = Locale("de", "DE")
        assertThat(CountryUtils.getDisplayCountry(CountryCode.create("DE"), currentLocale))
            .isEqualTo("Deutschland")
    }

    @Test
    fun countryIsAvailableEvenWhenNotReturnedFrom_getISOCountries() {
        // https://github.com/stripe/stripe-android/issues/6501
        // We used to use Locale.getISOCountries instead of our hardcoded set.
        // Some countries (notably Kosovo) wasn't available on some older Android versions.
        assertThat(CountryUtils.getDisplayCountry(CountryCode.create("XK"), Locale.US))
            .isEqualTo("Kosovo")
        val country = CountryUtils.getCountryByCode(CountryCode.create("XK"), Locale.US)
        assertThat(country).isEqualTo(Country("XK", "Kosovo"))
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
    fun `formatNameForSorting does nothing to already formatted strings`() {
        val input = "aland"
        val expectedOutput = "aland"

        assertThat(CountryUtils.normalize(input)).isEqualTo(expectedOutput)
    }

    @Test
    fun `formatNameForSorting removes accents and diacritics`() {
        val input = "Dziękuję Åland"
        val expectedOutput = "dziekuje aland"

        assertThat(CountryUtils.normalize(input)).isEqualTo(expectedOutput)
    }

    @Test
    fun `formatNameForSorting removes capitalization`() {
        val input = "Aland"
        val expectedOutput = "aland"

        assertThat(CountryUtils.normalize(input)).isEqualTo(expectedOutput)
    }

    @Test
    fun `formatNameForSorting removes non alphanumeric characters`() {
        val input = "aºland1!!!"
        val expectedOutput = "aland"

        assertThat(CountryUtils.normalize(input)).isEqualTo(expectedOutput)
    }
}
