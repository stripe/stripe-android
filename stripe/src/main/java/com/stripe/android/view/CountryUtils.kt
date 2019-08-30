package com.stripe.android.view

import java.util.Locale

internal object CountryUtils {

    private val NO_POSTAL_CODE_COUNTRIES =
        arrayOf("AE", "AG", "AN", "AO", "AW", "BF", "BI", "BJ", "BO", "BS", "BW", "BZ", "CD", "CF", "CG", "CI", "CK", "CM", "DJ", "DM", "ER", "FJ", "GD", "GH", "GM", "GN", "GQ", "GY", "HK", "IE", "JM", "KE", "KI", "KM", "KN", "KP", "LC", "ML", "MO", "MR", "MS", "MU", "MW", "NR", "NU", "PA", "QA", "RW", "SB", "SC", "SL", "SO", "SR", "ST", "SY", "TF", "TK", "TL", "TO", "TT", "TV", "TZ", "UG", "VU", "YE", "ZA", "ZW")
    private val NO_POSTAL_CODE_COUNTRIES_SET = setOf(*NO_POSTAL_CODE_COUNTRIES)

    private val COUNTRY_NAMES_TO_CODES: Map<String, String>
        get() {
            return Locale.getISOCountries()
                .associateBy { Locale("", it).displayCountry }
        }

    @JvmStatic
    fun getCountryCode(countryName: String?): String? {
        return COUNTRY_NAMES_TO_CODES[countryName]
    }

    @JvmStatic
    fun getOrderedCountries(currentLocale: Locale): List<String> {
        // Show user's current locale first, followed by countries alphabetized by display name
        return listOf(currentLocale.displayCountry)
            .plus(
                COUNTRY_NAMES_TO_CODES.keys.toList()
                    .sortedWith(compareBy { it.toLowerCase(Locale.ROOT) })
                    .minus(currentLocale.displayCountry)
            )
    }

    @JvmStatic
    fun doesCountryUsePostalCode(countryCode: String): Boolean {
        return !NO_POSTAL_CODE_COUNTRIES_SET.contains(countryCode)
    }
}
