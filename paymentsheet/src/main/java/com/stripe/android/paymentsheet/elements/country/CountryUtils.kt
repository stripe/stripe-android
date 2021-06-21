package com.stripe.android.paymentsheet.elements.country

import java.util.Locale

internal object CountryUtils {
    private fun localizedCountries(currentLocale: Locale) =
        Locale.getISOCountries().map { code ->
            Country(
                CountryCode.create(code),
                Locale("", code).getDisplayCountry(currentLocale)
            )
        }

    @JvmSynthetic
    internal fun getCountryByCode(countryCode: CountryCode?, currentLocale: Locale): Country? {
        return localizedCountries(currentLocale).firstOrNull {
            it.code == countryCode
        }
    }

    @JvmSynthetic
    internal fun getCountryCodeByName(countryName: String, currentLocale: Locale): CountryCode? {
        return localizedCountries(currentLocale).firstOrNull { it.name == countryName }?.code
    }

    @JvmSynthetic
    internal fun getOrderedCountries(currentLocale: Locale): List<Country> {
        // Show user's current locale first, followed by countries alphabetized by display name
        return listOfNotNull(getCountryByCode(currentLocale.getCountryCode(), currentLocale))
            .plus(
                localizedCountries(currentLocale)
                    .sortedBy { it.name.lowercase() }
                    .filterNot { it.code == currentLocale.getCountryCode() }
            )
    }
}
