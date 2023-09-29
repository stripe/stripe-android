package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CurrencySettingsDefinition : StringSettingDefinition(
    key = "currency",
    displayName = "Currency",
) {
    private val usd = Option("USD", "USD")

    override val defaultValue: String = usd.value

    override val options: List<Option<String>> = listOf(
        Option("AUD", "AUD"),
        Option("EUR", "EUR"),
        Option("GBP", "GBP"),
        usd,
        Option("INR", "INR"),
        Option("PLN", "PLN"),
        Option("SGD", "SGD"),
        Option("MYR", "MYR"),
        Option("MXN", "MXN"),
        Option("BRL", "BRL"),
        Option("JPY", "JPY"),
        Option("SEK", "SEK"),
    )

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.currency(value)
    }
}
