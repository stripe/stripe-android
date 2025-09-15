package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlag

internal open class FeatureFlagSettingsDefinition(
    private val featureFlag: FeatureFlag,
    private val allowedIntegrationTypes: List<PlaygroundConfigurationData.IntegrationType> =
        PlaygroundConfigurationData.IntegrationType.entries,
) : BooleanSettingsDefinition(
    key = "featureFlag_${featureFlag.name}",
    displayName = featureFlag.name,
    defaultValue = false,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return allowedIntegrationTypes.contains(configurationData.integrationType)
    }

    override fun setValue(value: Boolean) {
        super.setValue(value)
        featureFlag.setEnabled(value)
    }
}
