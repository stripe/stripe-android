package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionSetAsDefaultSettingsDefinition :
    PlaygroundSettingDefinition<FeatureState>,
    PlaygroundSettingDefinition.Saveable<FeatureState> by EnumSaveable(
        key = "customer_session_set_as_default",
        values = FeatureState.entries.toTypedArray(),
        defaultValue = FeatureState.Disabled,
    ),
    PlaygroundSettingDefinition.Displayable<FeatureState> {
    override val displayName: String = "Customer Session Set As Default Feature"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<FeatureState>> {
        return FeatureState.entries.map { featureState ->
            option(name = featureState.name, value = featureState)
        }
    }

    override fun configure(value: FeatureState, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.paymentMethodSetAsDefaultFeature(value)
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
