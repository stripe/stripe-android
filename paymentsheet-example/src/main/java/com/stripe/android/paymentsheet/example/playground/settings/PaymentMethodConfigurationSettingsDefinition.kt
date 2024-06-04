package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object PaymentMethodConfigurationSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "paymentMethodConfigurationId"
    override val displayName: String = "Payment Method Configuration ID"
    override val defaultValue: String = ""

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: String,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        if (value.isNotEmpty()) {
            checkoutRequestBuilder.paymentMethodConfigurationId(value)
        }
    }
}
