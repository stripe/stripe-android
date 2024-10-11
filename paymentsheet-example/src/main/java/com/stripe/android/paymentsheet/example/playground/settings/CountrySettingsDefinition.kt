package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import java.util.Locale

internal object CountrySettingsDefinition :
    PlaygroundSettingDefinition<Country>,
    PlaygroundSettingDefinition.Saveable<Country> by EnumSaveable(
        key = "country",
        values = Country.entries.toTypedArray(),
        defaultValue = Country.US,
    ),
    PlaygroundSettingDefinition.Displayable<Country> {
    private val supportedPaymentFlowCountries = Country.entries.map { it.value }.toSet()
    private val supportedCustomerFlowCountries = setOf(
        Country.US.value,
        Country.FR.value,
    )

    override val displayName: String = "Merchant"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<Country>> {
        val supportedCountries = if (configurationData.integrationType.isPaymentFlow()) {
            supportedPaymentFlowCountries
        } else {
            supportedCustomerFlowCountries
        }

        return CountryUtils.getOrderedCountries(Locale.getDefault()).filter { country ->
            country.code.value in supportedCountries
        }.map { country ->
            option(country.name, convertToValue(country.code.value))
        }.toList()
    }

    override fun configure(value: Country, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.merchantCountryCode(value.value)
    }

    override fun configure(
        value: Country,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        customerEphemeralKeyRequestBuilder.merchantCountryCode(value.value)
    }

    override fun valueUpdated(value: Country, playgroundSettings: PlaygroundSettings) {
        // When the country changes via the UI, update the currency to be the default currency for
        // that country.
        val countriesToCurrencyMap: Map<Country, Currency> = mapOf(
            Country.GB to Currency.GBP,
            Country.FR to Currency.EUR,
            Country.AU to Currency.AUD,
            Country.US to Currency.USD,
            Country.IN to Currency.INR,
            Country.SG to Currency.SGD,
            Country.MY to Currency.MYR,
            Country.MX to Currency.MXN,
            Country.BR to Currency.BRL,
            Country.JP to Currency.JPY,
            Country.CN to Currency.CNY,
            Country.DE to Currency.EUR,
            Country.IT to Currency.EUR,
            Country.TH to Currency.THB,
        )

        countriesToCurrencyMap[value]?.let { currency ->
            playgroundSettings[CurrencySettingsDefinition] = currency
        }

        // When the changes via the UI, reset the customer.
        if (playgroundSettings[CustomerSettingsDefinition].value is CustomerType.Existing) {
            playgroundSettings[CustomerSettingsDefinition] = CustomerType.NEW
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
    CN("CN"),
    DE("DE"),
    IT("IT"),
    TH("TH"),
}
