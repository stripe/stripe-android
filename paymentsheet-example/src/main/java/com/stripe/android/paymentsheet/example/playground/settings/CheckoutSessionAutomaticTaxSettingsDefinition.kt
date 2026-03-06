package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CheckoutSessionAutomaticTaxSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Automatic Tax",
    key = "checkout_session_automatic_tax"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.automaticTax(value)
    }
}
