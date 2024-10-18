package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CurrencySettingsDefinition :
    PlaygroundSettingDefinition<Currency>,
    PlaygroundSettingDefinition.Saveable<Currency> by EnumSaveable(
        key = "currency",
        values = Currency.entries.toTypedArray(),
        defaultValue = Currency.USD,
    ),
    PlaygroundSettingDefinition.Displayable<Currency> {
    override val displayName: String = "Currency"

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = Currency.entries.map {
        option(it.displayName, it)
    }

    override fun configure(value: Currency, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.currency(value.value)
    }
}

enum class Currency(val displayName: String, override val value: String) : ValueEnum {
    AUD("AUD", "aud"),
    EUR("EUR", "eur"),
    GBP("GBP", "gbp"),
    USD("USD", "usd"),
    INR("INR", "inr"),
    PLN("PLN", "pln"),
    SGD("SGD", "sgd"),
    MYR("MYR", "myr"),
    MXN("MXN", "mxn"),
    BRL("BRL", "brl"),
    JPY("JPY", "jpy"),
    SEK("SEK", "sek"),
    CNY("CNY", "cny"),
    CHF("CHF", "chf"),
    THB("THB", "thb"),
}
