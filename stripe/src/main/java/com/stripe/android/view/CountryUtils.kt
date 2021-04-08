package com.stripe.android.view

import java.util.Locale

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
            Country(code, Locale("", code).getDisplayCountry(currentLocale))
        }

    @JvmSynthetic
    fun getDisplayCountry(countryCode: String, currentLocale: Locale): String =
        getCountryByCode(countryCode, currentLocale)?.name
            ?: Locale("", countryCode).getDisplayCountry(currentLocale)

    @JvmSynthetic
    internal fun getCountryByName(countryName: String, currentLocale: Locale): Country? {
        return localizedCountries(currentLocale).firstOrNull { it.name == countryName }
    }

    @JvmSynthetic
    internal fun getCountryByCode(countryCode: String, currentLocale: Locale): Country? {
        return localizedCountries(currentLocale).firstOrNull { it.code == countryCode }
    }

    @JvmSynthetic
    internal fun getOrderedCountries(currentLocale: Locale): List<Country> {
        // Show user's current locale first, followed by countries alphabetized by display name
        return listOfNotNull(getCountryByCode(currentLocale.country, currentLocale))
            .plus(
                localizedCountries(currentLocale)
                    .sortedBy { it.name.toLowerCase(Locale.ROOT) }
                    .filterNot { it.code == currentLocale.country }
            )
    }

    @JvmSynthetic
    internal fun doesCountryUsePostalCode(countryCode: String): Boolean {
        return !NO_POSTAL_CODE_COUNTRIES.contains(countryCode.toUpperCase(Locale.ROOT))
    }
}
