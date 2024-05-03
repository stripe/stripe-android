package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object ShippingAddressSettingsDefinition : PlaygroundSettingDefinition<AddressDetails?> {
    override val defaultValue: AddressDetails? = null

    override fun configure(
        value: AddressDetails?,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.shippingDetails(value)
    }
}
