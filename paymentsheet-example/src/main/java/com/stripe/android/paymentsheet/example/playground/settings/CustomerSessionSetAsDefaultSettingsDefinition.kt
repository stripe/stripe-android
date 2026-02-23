package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionSetAsDefaultSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Customer Session Set As Default Feature",
    key = "customer_session_set_as_default"
) {
    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.paymentMethodSetAsDefaultFeature(FeatureState.fromBoolean(value))
    }

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow()) {
            return false
        }

        return settings[CustomerSessionSettingsDefinition] == true
    }
}
