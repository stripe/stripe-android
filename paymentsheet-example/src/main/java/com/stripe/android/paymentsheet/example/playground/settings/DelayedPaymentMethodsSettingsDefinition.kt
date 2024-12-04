package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object DelayedPaymentMethodsSettingsDefinition : BooleanSettingsDefinition(
    key = "delayedPaymentMethods",
    displayName = "Delayed Payment Methods",
    defaultValue = true,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.allowsDelayedPaymentMethods(value)
    }

    @ExperimentalEmbeddedPaymentElementApi
    override fun configure(
        value: Boolean,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.allowsDelayedPaymentMethods(value)
    }
}
