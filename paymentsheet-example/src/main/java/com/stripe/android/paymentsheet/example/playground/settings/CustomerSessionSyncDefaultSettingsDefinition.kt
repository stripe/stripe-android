package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionSyncDefaultSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Customer Session Sync Default Feature",
    key = "customer_session_sync_default"
) {
    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        PlaygroundSettingDefinition.Displayable.Option("Enabled", true),
        PlaygroundSettingDefinition.Displayable.Option("Disabled", false),
    )

    override fun configure(value: Boolean, customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder) {
        if (value) {
            customerEphemeralKeyRequestBuilder.paymentMethodSyncDefaultFeature(FeatureState.Enabled)
        } else {
            customerEphemeralKeyRequestBuilder.paymentMethodSyncDefaultFeature(FeatureState.Disabled)
        }
    }

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isCustomerFlow()
    }
}
