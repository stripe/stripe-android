package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object PaymentMethodConfigurationSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "paymentMethodConfigurationID"
    override val displayName: String = "Payment Method Configuration ID"
    override val defaultValue: String = ""
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<String>> = emptyList()

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun configure(
        value: String,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        if (value.isNotEmpty()) {
            checkoutRequestBuilder.paymentMethodConfiguration(value)
        }
    }
}
