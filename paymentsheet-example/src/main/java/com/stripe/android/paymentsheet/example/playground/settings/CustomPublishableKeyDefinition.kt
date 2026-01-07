package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CustomPublishableKeyDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "customPublishableKey"
    override val displayName: String = "Custom Publishable Key"
    override val defaultValue: String = ""

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>
    ): Boolean {
        return settings[MerchantSettingsDefinition] == Merchant.Custom
    }

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value.isNotEmpty()) {
            checkoutRequestBuilder.customPublishableKey(value)
        }
    }
}
