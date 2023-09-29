package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CurrencySettingsDefinition : StringSettingDefinition(
    key = "currency",
    displayName = "Currency",
) {
    private val usd = Option("USD", "usd")

    override val defaultValue: String = usd.value

    override val options: List<Option<String>> = listOf(
        Option("AUD", "aud"),
        Option("EUR", "eur"),
        Option("GBP", "gbp"),
        usd,
        Option("INR", "inr"),
        Option("PLN", "pln"),
        Option("SGD", "sgd"),
        Option("MYR", "myr"),
        Option("MXN", "mxn"),
        Option("BRL", "brl"),
        Option("JPY", "jpy"),
        Option("SEK", "sek"),
    )

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.currency(value)
    }
}
