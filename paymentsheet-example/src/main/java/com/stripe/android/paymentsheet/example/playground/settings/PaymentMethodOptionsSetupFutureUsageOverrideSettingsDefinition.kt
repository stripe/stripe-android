package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.utils.stringValueToMap

internal object PaymentMethodOptionsSetupFutureUsageOverrideSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Displayable<String>,
    PlaygroundSettingDefinition.Saveable<String> {

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        stringValueToMap(value)?.let {
            checkoutRequestBuilder.overridePaymentMethodOptionsSetupFutureUsage(valuesMap = it)
        }
    }

    override val key: String = "pmoSetupFutureUsageOverride"
    override val displayName: String = "PMO SFU Override (comma separated, code:sfu)"
    override val defaultValue: String = ""

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value
}
