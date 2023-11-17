package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import java.util.Locale

internal object CountrySettingsDefinition :
    PlaygroundSettingDefinition<Country>,
    PlaygroundSettingDefinition.Saveable<Country> by EnumSaveable(
        key = "country",
        values = Country.values(),
        defaultValue = Country.US,
    ),
    PlaygroundSettingDefinition.Displayable<Country> {
    private val supportedCountries = Country.values().map { it.value }.toSet()

    override val displayName: String = "Merchant"
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<Country>> =
        CountryUtils.getOrderedCountries(Locale.getDefault()).filter { country ->
            country.code.value in supportedCountries
        }.map { country ->
            option(country.name, convertToValue(country.code.value))
        }.toList()

    override fun configure(value: Country, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.merchantCountryCode(value.value)
    }

    override fun valueUpdated(value: Country, playgroundSettings: PlaygroundSettings) {
        // When the country changes via the UI, update the currency to be the default currency for
        // that country.
        when (value) {
            Country.GB -> Currency.GBP
            Country.FR -> Currency.EUR
            Country.AU -> Currency.AUD
            Country.US -> Currency.USD
            Country.IN -> Currency.INR
            Country.SG -> Currency.SGD
            Country.MY -> Currency.MYR
            Country.MX -> Currency.MXN
            Country.BR -> Currency.BRL
            Country.JP -> Currency.JPY
        }.let { currency ->
            playgroundSettings[CurrencySettingsDefinition] = currency
        }
    }
}

enum class Country(override val value: String) : ValueEnum {
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
