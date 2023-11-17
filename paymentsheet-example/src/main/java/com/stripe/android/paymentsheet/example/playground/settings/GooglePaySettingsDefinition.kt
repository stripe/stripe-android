package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object GooglePaySettingsDefinition : BooleanSettingsDefinition(
    key = "googlePay",
    displayName = "Google Pay",
    defaultValue = true,
) {
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        if (value) {
            configurationBuilder.googlePay(
                PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = playgroundState.countryCode.value,
                    currencyCode = playgroundState.currencyCode.value,
                )
            )
        }
    }
}
