package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object ShippingAddressSettingsDefinition : PlaygroundSettingDefinition<AddressDetails?>(
    key = "shippingAddress",
    displayName = "" // Not displayed.
) {
    override val defaultValue: AddressDetails? = null
    override val options: List<Option<AddressDetails?>> = emptyList()

    override fun convertToValue(value: String): AddressDetails? {
        return null
    }

    override fun convertToString(value: AddressDetails?): String {
        return ""
    }

    override fun configure(
        value: AddressDetails?,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData
    ) {
        configurationBuilder.shippingDetails(value)
    }
}
