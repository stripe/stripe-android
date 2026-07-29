package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object UseStripeHostedAutocompleteSettingsDefinition : BooleanSettingsDefinition(
    key = "useStripeHostedAutocomplete",
    displayName = "Use Stripe-hosted endpoints",
    defaultValue = false,
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.PaymentSheet ||
            configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.FlowController ||
            configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.FlowControllerWithSpt
    }

    override fun setValue(value: Boolean) {
        FeatureFlags.forceStripeHostedAutocomplete.setEnabled(value)
    }
}
