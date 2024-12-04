package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object ShippingAddressSettingsDefinition : PlaygroundSettingDefinition<AddressDetails?> {
    override val defaultValue: AddressDetails? = null

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: AddressDetails?,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.shippingDetails(value)
    }

    @ExperimentalEmbeddedPaymentElementApi
    override fun configure(
        value: AddressDetails?,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.shippingDetails(value)
    }
}
