package com.stripe.android.core.model

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.text.Normalizer
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet -- this still auto-completes
object CountryUtils {
    private val CARD_POSTAL_CODE_COUNTRIES = setOf(
        "US",
        "GB",
        "CA"
    )

    private fun localizedCountries(currentLocale: Locale) =
        Locale.getISOCountries().map { code ->
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

    @VisibleForTesting
    internal fun formatNameForSorting(name: String): String {
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
                    .sortedBy { formatNameForSorting(it.name) }
            )

            cachedOrderedLocalizedCountries
        }
    }
}
