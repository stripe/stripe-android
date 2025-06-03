package com.stripe.android.paymentsheet.example.playground.settings

internal object WalletButtonsSettingsDefinition : BooleanSettingsDefinition(
    key = "WalletButtons",
    displayName = "Show Wallet Buttons",
    defaultValue = false
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.Embedded
    }
}
