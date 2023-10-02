package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import java.util.Locale

internal object CountrySettingsDefinition :
    PlaygroundSettingDefinition<CountrySettingsDefinition.Country>(
        key = "country",
        displayName = "Merchant",
    ) {
    private val supportedCountries = Country.values().map { it.value }.toSet()

    override val defaultValue: Country = Country.US
    override val options: List<Option<Country>> =
        CountryUtils.getOrderedCountries(Locale.getDefault()).filter { country ->
            country.code.value in supportedCountries
        }.map { country ->
            Option(country.name, convertToValue(country.code.value))
        }.toList()

    override fun convertToValue(value: String): Country {
        return Country.values().firstOrNull { it.value == value } ?: defaultValue
    }

    override fun convertToString(value: Country): String {
        return value.value
    }

    override fun configure(value: Country, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.merchantCountryCode(value.value)
    }

    override fun valueUpdated(value: Country, playgroundSettings: PlaygroundSettings) {
        // When the country changes via the UI, update the currency to be the default currency for
        // that country.
        when (value) {
            Country.GB -> CurrencySettingsDefinition.Currency.GBP
            Country.FR -> CurrencySettingsDefinition.Currency.EUR
            Country.AU -> CurrencySettingsDefinition.Currency.AUD
            Country.US -> CurrencySettingsDefinition.Currency.USD
            Country.IN -> CurrencySettingsDefinition.Currency.INR
            Country.SG -> CurrencySettingsDefinition.Currency.SGD
            Country.MY -> CurrencySettingsDefinition.Currency.MYR
            Country.MX -> CurrencySettingsDefinition.Currency.MXN
            Country.BR -> CurrencySettingsDefinition.Currency.BRL
            Country.JP -> CurrencySettingsDefinition.Currency.JPY
        }.let { currency ->
            playgroundSettings[CurrencySettingsDefinition] = currency
        }
    }

    enum class Country(val value: String) {
        US("US"),
        GB("GB"),
        AU("AU"),
        FR("FR"),
        IN("IN"),
        SG("SG"),
        MY("MY"),
        MX("MX"),
        BR("BR"),
        JP("JP"),
    }
}
