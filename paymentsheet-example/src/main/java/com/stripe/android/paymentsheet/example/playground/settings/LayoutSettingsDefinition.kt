package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.ExperimentalPaymentMethodLayoutApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object LayoutSettingsDefinition : BooleanSettingsDefinition(
    key = "layout",
    displayName = "Vertical Mode",
    defaultValue = false,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    @OptIn(ExperimentalPaymentMethodLayoutApi::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.paymentMethodLayout(
            if (value) {
                PaymentSheet.PaymentMethodLayout.Vertical
            } else {
                PaymentSheet.PaymentMethodLayout.Horizontal
            }
        )
    }
}
