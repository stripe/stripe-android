package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CustomerEmailSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "customerEmail"
    override val displayName: String = "Customer email"
    override val defaultValue: String = ""

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        val customerType = settings[CustomerSettingsDefinition]
        val isCheckoutSession =
            settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
        return customerType == CustomerType.NEW ||
            (customerType == CustomerType.GUEST && isCheckoutSession)
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value.isNotEmpty()) {
            checkoutRequestBuilder.customerEmail(value)
        }
    }
}
