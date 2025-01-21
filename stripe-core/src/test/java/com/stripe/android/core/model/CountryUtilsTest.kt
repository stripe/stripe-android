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
    fun getOrderedCountriesIn_en_US() {
        val  orderedCountries =  CountryUtils.getOrderedCountries(Locale("en", "US"))
        // Check the first 20 countries in the list.
        assertThat(orderedCountries.subList(0,20).map { it.name }.toList()).isEqualTo(
            listOf("United States", "Afghanistan", "Åland Islands", "Albania", "Algeria",
                "Andorra", "Angola", "Anguilla", "Antarctica", "Antigua & Barbuda",
                "Argentina", "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan",
                "Bahamas", "Bahrain", "Bangladesh", "Barbados")
        )
    }

    @Test
    fun getOrderedCountriesIn_en_SG() {
        val  orderedCountries =  CountryUtils.getOrderedCountries(Locale("en", "SG"))
        // Check the first 20 countries in the list.
        assertThat(orderedCountries.subList(0,20).map { it.name }.toList()).isEqualTo(
            listOf("Singapore", "Afghanistan", "Åland Islands", "Albania", "Algeria",
                "Andorra", "Angola", "Anguilla", "Antarctica", "Antigua & Barbuda",
                "Argentina", "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan",
                "Bahamas", "Bahrain", "Bangladesh", "Barbados")
        )
    }

    @Test
    fun getOrderedCountriesIn_fr_FR() {
        val  orderedCountries =  CountryUtils.getOrderedCountries(Locale("fr", "FR"))
        // Check the first 20 countries in the list.
        assertThat(orderedCountries.subList(0,20).map { it.name }.toList()).isEqualTo(
            listOf("France", "Afghanistan", "Afrique du Sud", "Albanie", "Algérie", "Allemagne", "Andorre",
                "Angola", "Anguilla", "Antarctique", "Antigua-et-Barbuda", "Arabie saoudite", "Argentine",
                "Arménie", "Aruba", "Australie", "Autriche", "Azerbaïdjan", "Bahamas", "Bahreïn")
        )
    }

    @Test
    fun getOrderedCountriesIn_de_DE() {
        val  orderedCountries =  CountryUtils.getOrderedCountries(Locale("de", "De"))
        // Check the first 20 countries in the list.
        assertThat(orderedCountries.subList(0,20).map { it.name }.toList()).isEqualTo(
            listOf("Deutschland", "Afghanistan", "Ägypten", "Ålandinseln", "Albanien",
                "Algerien", "Andorra", "Angola", "Anguilla", "Antarktis", "Antigua und Barbuda",
                "Äquatorialguinea", "Argentinien", "Armenien", "Aruba", "Aserbaidschan", "Äthiopien",
                "Australien", "Bahamas", "Bahrain")
        )
    }

    @Test
    fun getOrderedCountriesIn_zh_CN() {
        val  orderedCountries =  CountryUtils.getOrderedCountries(Locale("zh", "CN"))
        // Check the first 20 countries in the list.
        assertThat(orderedCountries.subList(0,20).map { it.name }.toList()).isEqualTo(
            listOf("中国", "阿尔巴尼亚", "阿尔及利亚", "阿富汗",
                "阿根廷", "阿拉伯联合酋长国", "阿鲁巴", "阿曼", "阿塞拜疆", "埃及", "埃塞俄比亚",
                "爱尔兰", "爱沙尼亚", "安道尔", "安哥拉", "安圭拉", "安提瓜和巴布达", "奥地利", "奥兰群岛",
                "澳大利亚")
        )
    }

    @Test
    fun getOrderedCountriesIn_ja_JP() {
        val  orderedCountries =  CountryUtils.getOrderedCountries(Locale("ja", "JP"))
        // Check the first 20 countries in the list.
        assertThat(orderedCountries.subList(0,20).map { it.name }.toList()).isEqualTo(
            listOf("日本", "アイスランド", "アイルランド", "アゼルバイジャン", "アフガニスタン", "アメリカ合衆国",
                "アラブ首長国連邦", "アルジェリア", "アルゼンチン", "アルバ", "アルバニア", "アルメニア", "アンギラ",
                "アンゴラ", "アンティグア・バーブーダ", "アンドラ", "イエメン", "イギリス", "イスラエル", "イタリア")
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
}
