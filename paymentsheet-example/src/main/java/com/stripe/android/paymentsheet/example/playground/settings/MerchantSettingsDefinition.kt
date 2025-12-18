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
    private val supportedPaymentFlowCountries = Merchant.entries.map { it.countryCode }.toSet()
    private val supportedCustomerFlowCountries = setOf(
        Merchant.US.countryCode,
        Merchant.FR.countryCode,
    )

    override val displayName: String = "Merchant"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>?,
    ): Boolean {
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
        customerEphemeralKeyRequestBuilder.merchantCountryCode(value.countryCode)
    }

    override fun valueUpdated(value: Merchant, playgroundSettings: PlaygroundSettings) {
        // When the merchant changes via the UI, update the currency to be the default currency for
        // that merchant.
        playgroundSettings[CurrencySettingsDefinition] = value.currency

        if (value != Merchant.US) {
            playgroundSettings[CustomerSessionOnBehalfOfSettingsDefinition] =
                CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf.NO_CONNECTED_ACCOUNT
        }

        // When the merchant changes via the UI, reset the customer.
        if (playgroundSettings[CustomerSettingsDefinition].value is CustomerType.Existing) {
            playgroundSettings[CustomerSettingsDefinition] = CustomerType.NEW
        }
    }

    private val Merchant.currency: Currency
        get() {
            return when (this) {
                Merchant.US -> Currency.USD
                Merchant.GB -> Currency.GBP
                Merchant.AU -> Currency.AUD
                Merchant.FR -> Currency.EUR
                Merchant.IN -> Currency.INR
                Merchant.SG -> Currency.SGD
                Merchant.MY -> Currency.MYR
                Merchant.MX -> Currency.MXN
                Merchant.BR -> Currency.BRL
                Merchant.JP -> Currency.JPY
                Merchant.CN -> Currency.CNY
                Merchant.DE -> Currency.EUR
                Merchant.IT -> Currency.EUR
                Merchant.TH -> Currency.THB
                Merchant.StripeShop -> Currency.USD
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

val Merchant.countryCode: String
    get() {
        return when (this) {
            Merchant.US -> value
            Merchant.GB -> value
            Merchant.AU -> value
            Merchant.FR -> value
            Merchant.IN -> value
            Merchant.SG -> value
            Merchant.MY -> value
            Merchant.MX -> value
            Merchant.BR -> value
            Merchant.JP -> value
            Merchant.CN -> value
            Merchant.DE -> value
            Merchant.IT -> value
            Merchant.TH -> value
            Merchant.StripeShop -> "US"
        }
    }
