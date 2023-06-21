package com.stripe.android.core.model

import androidx.annotation.RestrictTo
import java.text.Normalizer
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet -- this still auto-completes
object CountryUtils {

    // This comes from: stripe-js-v3/blob/master/src/lib/shared/checkoutSupportedCountries.js
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val supportedBillingCountries = setOf(
        "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AT", "AU", "AW", "AX",
        "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL", "BM", "BN", "BO",
        "BQ", "BR", "BS", "BT", "BV", "BW", "BY", "BZ", "CA", "CD", "CF", "CG", "CH", "CI",
        "CK", "CL", "CM", "CN", "CO", "CR", "CV", "CW", "CY", "CZ", "DE", "DJ", "DK", "DM",
        "DO", "DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK", "FO", "FR",
        "GA", "GB", "GD", "GE", "GF", "GG", "GH", "GI", "GL", "GM", "GN", "GP", "GQ", "GR",
        "GS", "GT", "GU", "GW", "GY", "HK", "HN", "HR", "HT", "HU", "ID", "IE", "IL", "IM",
        "IN", "IO", "IQ", "IS", "IT", "JE", "JM", "JO", "JP", "KE", "KG", "KH", "KI", "KM",
        "KN", "KR", "KW", "KY", "KZ", "LA", "LB", "LC", "LI", "LK", "LR", "LS", "LT", "LU",
        "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MK", "ML", "MM", "MN", "MO", "MQ",
        "MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA", "NC", "NE", "NG", "NI",
        "NL", "NO", "NP", "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG", "PH", "PK", "PL",
        "PM", "PN", "PR", "PS", "PT", "PY", "QA", "RE", "RO", "RS", "RU", "RW", "SA", "SB",
        "SC", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SR", "SS", "ST",
        "SV", "SX", "SZ", "TA", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM", "TN",
        "TO", "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "US", "UY", "UZ", "VA", "VC", "VE",
        "VG", "VN", "VU", "WF", "WS", "XK", "YE", "YT", "ZA", "ZM", "ZW"
    )

    private val CARD_POSTAL_CODE_COUNTRIES = setOf(
        "US",
        "GB",
        "CA"
    )

    private fun localizedCountries(currentLocale: Locale) = supportedBillingCountries.map { code ->
        Country(
            CountryCode.create(code),
            Locale("", code).getDisplayCountry(currentLocale)
        )
    }

    @JvmSynthetic
    fun getDisplayCountry(countryCode: CountryCode, currentLocale: Locale): String =
        getCountryByCode(countryCode, currentLocale)?.name
            ?: Locale("", countryCode.value).getDisplayCountry(currentLocale)

    @JvmSynthetic
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getCountryCodeByName(countryName: String, currentLocale: Locale): CountryCode? {
        return getSortedLocalizedCountries(currentLocale).firstOrNull { it.name == countryName }?.code
    }

    @JvmSynthetic
    fun getCountryByCode(countryCode: CountryCode?, currentLocale: Locale): Country? {
        return getSortedLocalizedCountries(currentLocale).firstOrNull {
            it.code == countryCode
        }
    }

    /**
     * Show user's current locale first, followed by countries alphabetized by display name
     */
    @JvmSynthetic
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getOrderedCountries(currentLocale: Locale) = getSortedLocalizedCountries(currentLocale)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun normalize(name: String): String {
        // Before normalization: åland islands
        // After normalization: aºland islands
        // After regex: aland islands
        return Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^A-Za-z ]".toRegex(), "")
            .replace("[^\\p{ASCII}]".toRegex(), "")
    }

    @Deprecated(
        message = "Use with parameter CountryCode",
        replaceWith = ReplaceWith(
            expression = "CountryUtils.doesCountryUsePostalCode(CountryCode.create(countryCode))",
            imports = ["com.stripe.android.model.CountryCode"]
        )
    )
    @JvmSynthetic
    fun doesCountryUsePostalCode(countryCode: String): Boolean {
        return CARD_POSTAL_CODE_COUNTRIES.contains(countryCode.uppercase())
    }

    @JvmSynthetic
    fun doesCountryUsePostalCode(countryCode: CountryCode): Boolean {
        return CARD_POSTAL_CODE_COUNTRIES.contains(countryCode.value)
    }

    private var cachedCountriesLocale: Locale? = null
    private var cachedOrderedLocalizedCountries: List<Country> = emptyList()

    /**
     * Load, sort and cache the list of localized countries, putting the current locale's country
     * at the top of the list.
     */
    private fun getSortedLocalizedCountries(currentLocale: Locale): List<Country> {
        return if (currentLocale == cachedCountriesLocale) {
            cachedOrderedLocalizedCountries
        } else {
            cachedCountriesLocale = currentLocale

            val localizedCountries = localizedCountries(currentLocale)
            cachedOrderedLocalizedCountries = listOfNotNull(
                localizedCountries.firstOrNull {
                    it.code == currentLocale.getCountryCode()
                }
            ).plus(
                localizedCountries
                    .filterNot { it.code == currentLocale.getCountryCode() }
                    .sortedBy { normalize(it.name) }
            )

            cachedOrderedLocalizedCountries
        }
    }
}
