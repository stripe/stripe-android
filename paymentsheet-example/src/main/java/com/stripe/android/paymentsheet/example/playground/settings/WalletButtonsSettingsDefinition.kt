package com.stripe.android.paymentsheet.example.playground.settings

internal object WalletButtonsSettingsDefinition : BooleanSettingsDefinition(
    key = "WalletButtons",
    displayName = "Show Wallet Buttons",
    defaultValue = false
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return when (configurationData.integrationType) {
            PlaygroundConfigurationData.IntegrationType.Embedded,
            PlaygroundConfigurationData.IntegrationType.FlowController -> true
            PlaygroundConfigurationData.IntegrationType.PaymentSheet,
            PlaygroundConfigurationData.IntegrationType.CustomerSheet -> false
        }
    }
}
