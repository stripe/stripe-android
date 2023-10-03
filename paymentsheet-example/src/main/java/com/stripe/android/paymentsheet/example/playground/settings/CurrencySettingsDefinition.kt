package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CurrencySettingsDefinition : PlaygroundSettingDefinition<CurrencySettingsDefinition.Currency>(
    key = "currency",
    displayName = "Currency",
) {
    override val defaultValue: Currency = Currency.USD

    override val options: List<Option<Currency>> = Currency.values().map {
        Option(it.displayName, it)
    }

    override fun convertToValue(value: String): Currency {
        return Currency.values().firstOrNull { it.value == value } ?: defaultValue
    }

    override fun convertToString(value: Currency): String {
        return value.value
    }

    override fun configure(value: Currency, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.currency(value.value)
    }

    enum class Currency(val displayName: String, val value: String) {
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
    }
}
