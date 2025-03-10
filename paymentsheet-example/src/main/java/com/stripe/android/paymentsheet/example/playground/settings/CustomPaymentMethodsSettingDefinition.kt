package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

@OptIn(ExperimentalEmbeddedPaymentElementApi::class, ExperimentalCustomPaymentMethodsApi::class)
internal object CustomPaymentMethodsSettingDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Enable Custom Payment Methods",
    key = "custom_payment_payments"
) {
    private val customPaymentMethodConfiguration = PaymentSheet.CustomPaymentMethodConfiguration(
        customPaymentMethodTypes = listOf(
            PaymentSheet.CustomPaymentMethodConfiguration.CustomPaymentMethodType(
                id = "cpmt_1QpIMNLu5o3P18Zpwln1Sm6I",
                subtitle = "Pay now with BufoPay",
                shouldCollectBillingDetails = true,
            )
        )
    )

    override fun configure(
        value: Boolean,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        if (value) {
            configurationBuilder.customPaymentMethodConfiguration(customPaymentMethodConfiguration)
        }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        if (value) {
            configurationBuilder.customPaymentMethodConfiguration(customPaymentMethodConfiguration)
        }
    }
}
