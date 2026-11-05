package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.uicore.elements.AddressFieldConfiguration

internal fun AddressLauncher.Configuration?.nameFieldConfiguration(): AddressFieldConfiguration {
    return when (this?.nameField) {
        AddressLauncher.NameField.Never ->
            AddressFieldConfiguration.HIDDEN
        AddressLauncher.NameField.Auto, null ->
            AddressFieldConfiguration.REQUIRED
    }
}

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
