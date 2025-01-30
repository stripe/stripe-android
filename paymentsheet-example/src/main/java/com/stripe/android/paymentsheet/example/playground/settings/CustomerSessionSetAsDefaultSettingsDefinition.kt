package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionSetAsDefaultSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Customer Session Set As Default Feature",
    key = "customer_session_set_as_default"
) {
    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        PlaygroundSettingDefinition.Displayable.Option("Enabled", true),
        PlaygroundSettingDefinition.Displayable.Option("Disabled", false),
    )

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value) {
            checkoutRequestBuilder.paymentMethodSetAsDefaultFeature(FeatureState.Enabled)
        } else {
            checkoutRequestBuilder.paymentMethodSetAsDefaultFeature(FeatureState.Disabled)
        }
    }
}
