package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object AmountSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "amount"
    override val displayName: String = "Amount (in cents)"
    override val defaultValue: String = "5099"

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        val amount = value.toLongOrNull()
        if (amount != null && amount > 0) {
            checkoutRequestBuilder.amount(amount)
        }
    }
}
