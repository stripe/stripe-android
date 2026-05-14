package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionRemoveLastSettingsDefinition :
    PlaygroundSettingDefinition<FeatureState>,
    PlaygroundSettingDefinition.Saveable<FeatureState> by EnumSaveable(
        key = "customer_session_payment_method_remove_last",
        values = FeatureState.entries.toTypedArray(),
        defaultValue = FeatureState.Enabled,
    ),
    PlaygroundSettingDefinition.Displayable<FeatureState> {
    override val displayName: String = "Customer Session Remove Last Payment Method"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<FeatureState>> {
        return FeatureState.entries.map { featureState ->
            option(name = featureState.name, value = featureState)
        }
    }

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow() && !configurationData.integrationType.isCustomerFlow()) {
            return false
        }

        return settings[CustomerSessionSettingsDefinition] == true
    }

    override fun configure(value: FeatureState, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.paymentMethodRemoveLastFeature(value)
    }

    override fun configure(
        value: FeatureState,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        customerEphemeralKeyRequestBuilder.paymentMethodRemoveLastFeature(value)
    }
}
