package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import java.util.Locale

internal object CountrySettingsDefinition : StringSettingDefinition(
    key = "country",
    displayName = "Merchant",
) {
    private val supportedCountries = setOf(
        "US",
        "GB",
        "AU",
        "FR",
        "IN",
        "SG",
        "MY",
        "MX",
        "BR",
        "JP",
    )

    override val defaultValue: String = "US"
    override val options: List<Option<String>> =
        CountryUtils.getOrderedCountries(Locale.getDefault()).filter { country ->
            country.code.value in supportedCountries
        }.map { country ->
            Option(country.name, country.code.value)
        }.toList()

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.merchantCountryCode(value)
    }

    override fun valueUpdated(value: String, playgroundSettings: PlaygroundSettings) {
        // When the country changes via the UI, update the currency to be the default currency for
        // that country.
        when (value) {
            "GB" -> "GBP"
            "FR" -> "EUR"
            "AU" -> "AUD"
            "US" -> "USD"
            "IN" -> "INR"
            "SG" -> "SGD"
            "MY" -> "MYR"
            "MX" -> "MXN"
            "BR" -> "BRL"
            "JP" -> "JPY"
            else -> null
        }?.let { currency ->
            playgroundSettings[CurrencySettingsDefinition] = currency
        }
    }
}
