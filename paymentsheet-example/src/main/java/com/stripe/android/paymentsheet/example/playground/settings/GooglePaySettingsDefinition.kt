package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
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
        playgroundState: PlaygroundState.Payment,
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

    @ExperimentalEmbeddedPaymentElementApi
    override fun configure(
        value: Boolean,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
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

    override fun configure(
        value: Boolean,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationBuilder.googlePayEnabled(value)
    }
}
