package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CheckoutSessionAdjustableQuantitySettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Adjustable Quantity",
    key = "checkout_session_adjustable_quantity"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.adjustableQuantity(value)
    }
}
