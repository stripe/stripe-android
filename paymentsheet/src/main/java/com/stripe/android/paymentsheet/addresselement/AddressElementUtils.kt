package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.uicore.elements.AddressFieldConfiguration

internal fun parsePhoneNumberConfig(
    configuration: AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration?
): AddressFieldConfiguration {
    return when (configuration) {
        AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN ->
            AddressFieldConfiguration.HIDDEN
        AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.OPTIONAL ->
            AddressFieldConfiguration.OPTIONAL
        AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.REQUIRED ->
            AddressFieldConfiguration.REQUIRED
        null -> AddressFieldConfiguration.OPTIONAL
    }
}
