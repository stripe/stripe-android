package com.stripe.android.paymentsheet.example.playground.settings

internal object WalletButtonsSettingsDefinition : BooleanSettingsDefinition(
    key = "WalletButtons",
    displayName = "Wallet Buttons",
    defaultValue = false
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.Embedded
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<Boolean>> {
        return listOf(
            PlaygroundSettingDefinition.Displayable.Option(
                name = "Show",
                value = true,
            ),
            PlaygroundSettingDefinition.Displayable.Option(
                name = "Hide",
                value = false,
            )
        )
    }
}
