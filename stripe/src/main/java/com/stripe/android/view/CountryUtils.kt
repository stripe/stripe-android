package com.stripe.android.view

import com.stripe.android.model.CountryCode
import com.stripe.android.model.getCountryCode
import java.util.Locale

internal object CountryUtils {

    internal val NO_POSTAL_CODE_COUNTRIES = setOf(
        "AE", "AG", "AN", "AO", "AW", "BF", "BI", "BJ", "BO", "BS", "BW", "BZ", "CD", "CF", "CG",
        "CI", "CK", "CM", "DJ", "DM", "ER", "FJ", "GD", "GH", "GM", "GN", "GQ", "GY", "HK", "IE",
        "JM", "KE", "KI", "KM", "KN", "KP", "LC", "ML", "MO", "MR", "MS", "MU", "MW", "NR", "NU",
        "PA", "QA", "RW", "SB", "SC", "SL", "SO", "SR", "ST", "SY", "TF", "TK", "TL", "TO", "TT",
        "TV", "TZ", "UG", "VU", "YE", "ZA", "ZW"
    )

    private val COUNTRIES: List<Country> =
        Locale.getISOCountries().map { code ->
            Country(CountryCode.create(code), Locale("", code).displayCountry)
        }

    @JvmSynthetic
    fun getDisplayCountry(countryCode: CountryCode): String =
        getCountryByCode(countryCode)?.name
            ?: Locale("", countryCode.value).displayCountry

    @JvmSynthetic
    internal fun getCountryCodeByName(countryName: String): CountryCode? {
        return COUNTRIES.firstOrNull { it.name == countryName }?.code
    }

    @JvmSynthetic
    internal fun getCountryByCode(countryCode: CountryCode): Country? {
        return COUNTRIES.firstOrNull { it.code == countryCode }
    }

    @JvmSynthetic
    internal fun getOrderedCountries(currentLocale: Locale): List<Country> {
        // Show user's current locale first, followed by countries alphabetized by display name
        return listOfNotNull(getCountryByCode(currentLocale.getCountryCode()))
            .plus(
                COUNTRIES
                    .sortedBy { it.name.toLowerCase(Locale.ROOT) }
                    .filterNot { it.code == currentLocale.getCountryCode() }
            )
    }

    @Deprecated(
        message = "Use with parameter CountryCode",
        replaceWith = ReplaceWith(
            expression = "CountryUtils.doesCountryUsePostalCode(CountryCode.create(countryCode))",
            imports = ["com.stripe.android.model.CountryCode"]
        )
    )
    @JvmSynthetic
    internal fun doesCountryUsePostalCode(countryCode: String): Boolean {
        return !NO_POSTAL_CODE_COUNTRIES.contains(countryCode.toUpperCase(Locale.ROOT))
    }

    @JvmSynthetic
    internal fun doesCountryUsePostalCode(countryCode: CountryCode): Boolean {
        return !NO_POSTAL_CODE_COUNTRIES.contains(countryCode.value)
    }
}
