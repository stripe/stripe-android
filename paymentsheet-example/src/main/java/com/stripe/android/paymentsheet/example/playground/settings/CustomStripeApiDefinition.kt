package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CustomStripeApiDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "customStripeApi"
    override val displayName: String = "Custom Stripe API"
    override val defaultValue: String = ""

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value.isNotEmpty()) {
            checkoutRequestBuilder.customStripeApi(value)
        }
    }
}
