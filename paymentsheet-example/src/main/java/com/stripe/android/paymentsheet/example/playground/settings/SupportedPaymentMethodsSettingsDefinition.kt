package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object SupportedPaymentMethodsSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Displayable<String>,
    PlaygroundSettingDefinition.Saveable<String> {

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value.isNotEmpty()) {
            checkoutRequestBuilder.supportedPaymentMethods(value.split(","))
        }
    }

    override val key: String = "supportedPaymentMethods"
    override val displayName: String = "Supported Payment Methods (comma separated)"
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<String>> = emptyList()
    override val defaultValue: String = ""

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value
}
