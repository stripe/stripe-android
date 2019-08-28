package com.stripe.android.view

import java.util.HashMap
import java.util.Locale

internal object CountryUtils {

    private val NO_POSTAL_CODE_COUNTRIES =
        arrayOf("AE", "AG", "AN", "AO", "AW", "BF", "BI", "BJ", "BO", "BS", "BW", "BZ", "CD", "CF", "CG", "CI", "CK", "CM", "DJ", "DM", "ER", "FJ", "GD", "GH", "GM", "GN", "GQ", "GY", "HK", "IE", "JM", "KE", "KI", "KM", "KN", "KP", "LC", "ML", "MO", "MR", "MS", "MU", "MW", "NR", "NU", "PA", "QA", "RW", "SB", "SC", "SL", "SO", "SR", "ST", "SY", "TF", "TK", "TL", "TO", "TT", "TV", "TZ", "UG", "VU", "YE", "ZA", "ZW")
    private val NO_POSTAL_CODE_COUNTRIES_SET = setOf(*NO_POSTAL_CODE_COUNTRIES)

    @JvmStatic
    val countryNameToCodeMap: Map<String, String>
        get() {
            val displayNameToCountryCode = HashMap<String, String>()
            for (countryCode in Locale.getISOCountries()) {
                val locale = Locale("", countryCode)
                displayNameToCountryCode[locale.displayCountry] = countryCode
            }
            return displayNameToCountryCode
        }

    @JvmStatic
    fun doesCountryUsePostalCode(countryCode: String): Boolean {
        return !NO_POSTAL_CODE_COUNTRIES_SET.contains(countryCode)
    }
}
