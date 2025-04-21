package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object PaymentMethodOptionsSetupFutureUsageOverrideSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Displayable<String>,
    PlaygroundSettingDefinition.Saveable<String> {

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        val map = value.split(",")
            .mapNotNull { entry ->
                entry.split(":").takeIf { it.size == 2 }?.let { pair ->
                    pair[0] to pair[1]
                }
            }.toMap()
        if (map.isNotEmpty()) checkoutRequestBuilder.overridePaymentMethodOptionsSetupFutureUsage(valuesMap = map)
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
