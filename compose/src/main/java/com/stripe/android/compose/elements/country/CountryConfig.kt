package com.stripe.android.compose.elements.country

import com.stripe.android.compose.elements.common.DropdownConfigInterface
import java.util.Locale

internal class CountryConfig : DropdownConfigInterface {
    override val debugLabel = "country"
    override val label = com.stripe.android.R.string.address_label_country

    private val countryToCountryCode = HashMap<String, String>().apply {
        COUNTRIES.forEach {
            put(it.name, it.code.value)
        }
    }

    private val countryCodeToCountry = HashMap<String, String>().apply {
        COUNTRIES.forEach {
            put(it.code.value, it.name)
        }
    }

    override fun convertToDisplay(paramFormatted: String?) =
        countryCodeToCountry[paramFormatted] ?: ""

    override fun convertToPaymentMethodParam(displayFormatted: String) =
        countryToCountryCode[displayFormatted]

    override fun getItems(): List<String> = COUNTRIES.map { it.name }

    companion object {
        // TODO: Need to determine the correct way to pass junit default locale
        val COUNTRIES: List<Country> =
            CountryUtils.getOrderedCountries(Locale.getDefault())
    }
}