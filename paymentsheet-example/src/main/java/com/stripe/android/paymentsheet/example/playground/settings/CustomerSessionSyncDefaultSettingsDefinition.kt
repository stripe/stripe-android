package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionSyncDefaultSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Customer Session Sync Default Feature",
    key = "customer_session_sync_default"
) {
    override fun configure(value: Boolean, customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder) {
        customerEphemeralKeyRequestBuilder.paymentMethodSyncDefaultFeature(FeatureState.fromBoolean(value))
    }

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType.isCustomerFlow()
    }
}
