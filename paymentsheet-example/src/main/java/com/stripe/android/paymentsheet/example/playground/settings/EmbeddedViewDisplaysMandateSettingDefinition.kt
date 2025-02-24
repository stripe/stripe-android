package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal object EmbeddedViewDisplaysMandateSettingDefinition : BooleanSettingsDefinition(
    key = "embeddedViewDisplaysMandate",
    displayName = "Embedded View Displays Mandate",
    defaultValue = true,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.Embedded
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.embeddedViewDisplaysMandateText(value)
    }
}
