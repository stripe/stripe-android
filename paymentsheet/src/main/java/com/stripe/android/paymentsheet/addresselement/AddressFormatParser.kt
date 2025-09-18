package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressFieldConfiguration
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.IdentifierSpec

internal class AddressFormatParser(
    private val config: AddressLauncher.Configuration?
) {
    fun parse(
        values: AddressDetails,
    ): Map<IdentifierSpec, String?> {
        val addressElement = createAddressElement(values)

        return addressElement.getFormFieldValueFlow().value.toMap().mapValues {
            it.value.value
        }
    }

    private fun createAddressElement(
        values: AddressDetails,
    ): AddressElement {
        return AddressElement(
            _identifier = IdentifierSpec.Generic("address"),
            rawValuesMap = values.toIdentifierMap(),
            countryCodes = config?.allowedCountries ?: CountryUtils.supportedBillingCountries,
            addressInputMode = AddressInputMode.NoAutocomplete(
                nameConfig = AddressFieldConfiguration.REQUIRED,
                phoneNumberConfig = parsePhoneNumberConfig(config?.additionalFields?.phone),
            ),
            sameAsShippingElement = null,
            shippingValuesMap = emptyMap()
        )
    }
}
