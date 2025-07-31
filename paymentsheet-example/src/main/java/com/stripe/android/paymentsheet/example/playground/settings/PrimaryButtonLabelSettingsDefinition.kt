package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object PrimaryButtonLabelSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "customPrimaryButtonLabel"
    override val displayName: String = "Custom primary button label"
    override val defaultValue: String = ""

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow() ||
            configurationData.integrationType.isSptFlow()
    }

    override fun configure(
        value: String,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        if (value.isNotEmpty()) {
            configurationBuilder.primaryButtonLabel(value)
        }
    }

    override fun configure(
        value: String,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        if (value.isNotEmpty()) {
            configurationBuilder.primaryButtonLabel(value)
        }
    }

    override fun configure(
        value: String,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        if (value.isNotEmpty()) {
            configurationBuilder.primaryButtonLabel(value)
        }
    }
}
