package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object AllowPromotionCodesSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Allow Promotion Codes",
    key = "allowPromotionCodes"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.allowPromotionCodes(value)
    }
}
