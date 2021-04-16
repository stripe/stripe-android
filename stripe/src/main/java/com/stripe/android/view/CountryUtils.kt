package com.stripe.android.view

import java.util.Locale

internal fun Locale.getCountryCode(): CountryCode = CountryCode(this.country)

internal object CountryUtils {

    internal val NO_POSTAL_CODE_COUNTRIES = setOf(
        "AE", "AG", "AN", "AO", "AW", "BF", "BI", "BJ", "BO", "BS", "BW", "BZ", "CD", "CF", "CG",
        "CI", "CK", "CM", "DJ", "DM", "ER", "FJ", "GD", "GH", "GM", "GN", "GQ", "GY", "HK", "IE",
        "JM", "KE", "KI", "KM", "KN", "KP", "LC", "ML", "MO", "MR", "MS", "MU", "MW", "NR", "NU",
        "PA", "QA", "RW", "SB", "SC", "SL", "SO", "SR", "ST", "SY", "TF", "TK", "TL", "TO", "TT",
        "TV", "TZ", "UG", "VU", "YE", "ZA", "ZW"
    )

    private fun localizedCountries(currentLocale: Locale) =
        Locale.getISOCountries().map { code ->
            Country(
                CountryCode(code),
                Locale("", code).getDisplayCountry(currentLocale)
            )
        }

    @JvmSynthetic
    fun getDisplayCountry(countryCode: CountryCode, currentLocale: Locale): String =
        getCountryByCode(countryCode, currentLocale)?.name
            ?: Locale("", countryCode.twoLetters).getDisplayCountry(currentLocale)

    @JvmSynthetic

    internal fun getCountryCodeByName(countryName: String, currentLocale: Locale): CountryCode? {
        return localizedCountries(currentLocale).firstOrNull { it.name == countryName }?.code
    }

    @JvmSynthetic
    internal fun getCountryByCode(countryCode: CountryCode?, currentLocale: Locale): Country? {
        return localizedCountries(currentLocale).firstOrNull {
            it.code == countryCode
        }
    }

    @JvmSynthetic
    internal fun getOrderedCountries(currentLocale: Locale): List<Country> {
        // Show user's current locale first, followed by countries alphabetized by display name
        return listOfNotNull(getCountryByCode(currentLocale.getCountryCode(), currentLocale))
            .plus(
                localizedCountries(currentLocale)
                    .sortedBy { it.name.toLowerCase(Locale.ROOT) }
                    .filterNot { it.code == currentLocale.getCountryCode() }
            )
    }

    @JvmSynthetic
    internal fun doesCountryUsePostalCode(countryCode: CountryCode): Boolean {
        return !NO_POSTAL_CODE_COUNTRIES.contains(countryCode.twoLetters.toUpperCase(Locale.ROOT))
    }
}
