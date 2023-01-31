package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressType
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * This is a special type of AddressElement that
 * removes fields from the address based on the country.  It
 * is only intended to be used with the card payment method.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardBillingAddressElement(
    identifier: IdentifierSpec,
    rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
    addressRepository: AddressRepository,
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    ),
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?
) : AddressElement(
    identifier,
    addressRepository,
    rawValuesMap,
    AddressType.Normal(),
    countryCodes,
    countryDropdownFieldController,
    sameAsShippingElement,
    shippingValuesMap
) {
    // Save for future use puts this in the controller rather than element
    // card and achv2 uses save for future use
    val hiddenIdentifiers: Flow<Set<IdentifierSpec>> =
        countryDropdownFieldController.rawFieldValue.map { countryCode ->
            when (countryCode) {
                "US", "GB", "CA" -> {
                    FieldType.values()
                        .filterNot { it == FieldType.PostalCode }
                        .map { it.identifierSpec }
                        .toSet()
                }
                else -> {
                    FieldType.values()
                        .map { it.identifierSpec }
                        .toSet()
                }
            }
        }
}
