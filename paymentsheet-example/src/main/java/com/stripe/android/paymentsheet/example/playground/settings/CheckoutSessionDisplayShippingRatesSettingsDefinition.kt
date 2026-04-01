package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CheckoutSessionDisplayShippingRatesSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Display Shipping Rates",
    key = "checkout_session_display_shipping_rates"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.displayShippingRates(value)
    }
}
