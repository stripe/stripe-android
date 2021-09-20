package com.stripe.android.view

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CountryCode
import com.stripe.android.model.getCountryCode
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
        assertThat(
            CountryUtils.getOrderedCountries(Locale.getDefault())[0].code
        ).isEqualTo(
            Locale.getDefault().getCountryCode()
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
    fun `getOrderedCountriesLocaleLanguage() in the language of the current locale`() {
        val currentLocale = Locale("de", "DE")
        val germany = CountryUtils.getOrderedCountries(currentLocale)
            .firstOrNull()

        // If the current locale is germany it should be first in the list, and the german
        // word for germany
        assertThat(germany?.name)
            .isEqualTo("Deutschland")
    }
}
