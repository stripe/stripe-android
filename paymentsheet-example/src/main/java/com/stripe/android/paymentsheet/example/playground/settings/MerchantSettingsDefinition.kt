package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import java.util.Locale

internal object MerchantSettingsDefinition :
    PlaygroundSettingDefinition<Merchant>,
    PlaygroundSettingDefinition.Saveable<Merchant> by EnumSaveable(
        key = "merchant",
        values = Merchant.entries.toTypedArray(),
        defaultValue = Merchant.US,
    ),
    PlaygroundSettingDefinition.Displayable<Merchant> {
    private val supportedPaymentFlowCountries = Merchant.entries.map { it.value }.toSet()
    private val supportedCustomerFlowCountries = setOf(
        Merchant.US.value,
        Merchant.FR.value,
    )

    override val displayName: String = "Merchant"

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow() ||
            configurationData.integrationType.isCustomerFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<Merchant>> {
        val supportedCountries = if (configurationData.integrationType.isPaymentFlow()) {
            supportedPaymentFlowCountries
        } else {
            supportedCustomerFlowCountries
        }

        return CountryUtils.getOrderedCountries(Locale.getDefault()).filter { country ->
            country.code.value in supportedCountries
        }.map { country ->
            option(country.name, convertToValue(country.code.value))
        }.toList() + listOf(
            option(Merchant.StripeShop.name, convertToValue(Merchant.StripeShop.value))
        )
    }

    override fun configure(value: Merchant, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.merchant(value.value)
    }

    override fun configure(
        value: Merchant,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        customerEphemeralKeyRequestBuilder.merchantCountryCode(value.value)
    }

    override fun valueUpdated(value: Merchant, playgroundSettings: PlaygroundSettings) {
        // When the country changes via the UI, update the currency to be the default currency for
        // that country.
        val countriesToCurrencyMap: Map<Merchant, Currency> = mapOf(
            Merchant.GB to Currency.GBP,
            Merchant.FR to Currency.EUR,
            Merchant.AU to Currency.AUD,
            Merchant.US to Currency.USD,
            Merchant.IN to Currency.INR,
            Merchant.SG to Currency.SGD,
            Merchant.MY to Currency.MYR,
            Merchant.MX to Currency.MXN,
            Merchant.BR to Currency.BRL,
            Merchant.JP to Currency.JPY,
            Merchant.CN to Currency.CNY,
            Merchant.DE to Currency.EUR,
            Merchant.IT to Currency.EUR,
            Merchant.TH to Currency.THB,
            Merchant.StripeShop to Currency.USD,
        )

        countriesToCurrencyMap[value]?.let { currency ->
            playgroundSettings[CurrencySettingsDefinition] = currency
        }

        if (value != Merchant.US) {
            playgroundSettings[CustomerSessionOnBehalfOfSettingsDefinition] =
                CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf.NO_CONNECTED_ACCOUNT
        }

        // When the changes via the UI, reset the customer.
        if (playgroundSettings[CustomerSettingsDefinition].value is CustomerType.Existing) {
            playgroundSettings[CustomerSettingsDefinition] = CustomerType.NEW
        }
    }
}

enum class Merchant(override val value: String) : ValueEnum {
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
    StripeShop("stripe_shop_test")
}
