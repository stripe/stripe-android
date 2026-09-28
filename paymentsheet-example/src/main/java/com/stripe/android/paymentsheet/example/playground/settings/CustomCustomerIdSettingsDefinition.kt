package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest

internal object CustomCustomerIdSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "customerId"
    override val displayName: String = "Customer ID"
    override val defaultValue: String = ""

    override fun convertToString(value: String): String = value

    override fun convertToValue(value: String): String = value

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return settings[CustomerSettingsDefinition] == CustomerType.CUSTOM
    }

    override fun configure(value: String, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.customer(value)
    }

    override fun configure(
        value: String,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        customerEphemeralKeyRequestBuilder.customerType(value)
    }
}
