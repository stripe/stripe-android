package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.ExperimentalPaymentMethodLayoutApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object LayoutSettingsDefinition : BooleanSettingsDefinition(
    key = "layout",
    displayName = "Vertical Mode",
    defaultValue = false,
) {
    @OptIn(ExperimentalPaymentMethodLayoutApi::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
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
