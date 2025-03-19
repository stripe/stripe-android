package com.stripe.android.paymentsheet.example.playground.settings

internal object EmbeddedTwoStepSettingsDefinition : BooleanSettingsDefinition(
    key = "embedded_2_step",
    displayName = "Embedded 2 Step Integration",
    defaultValue = false,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.Embedded
    }
}
