package com.stripe.android.paymentsheet.example.playground.settings

internal object UseApiConfigurationSettingsDefinition : BooleanSettingsDefinition(
    key = "useApiConfiguration",
    displayName = "Use ApiConfiguration",
    defaultValue = true,
) {
    fun isEnabled(snapshot: PlaygroundSettings.Snapshot): Boolean {
        return snapshot[this] && supports(snapshot.configurationData)
    }

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return supports(configurationData)
    }

    private fun supports(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType in setOf(
            PlaygroundConfigurationData.IntegrationType.PaymentSheet,
            PlaygroundConfigurationData.IntegrationType.FlowController,
            PlaygroundConfigurationData.IntegrationType.Embedded,
        )
    }
}
