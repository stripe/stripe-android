package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.address.FieldType
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
    )
) : AddressElement(
    identifier,
    addressRepository,
    rawValuesMap,
    AddressType.Normal(),
    countryCodes,
    countryDropdownFieldController
) {
    // Save for future use puts this in the controller rather than element
    val hiddenIdentifiers: Flow<List<IdentifierSpec>> =
        countryDropdownFieldController.rawFieldValue.map { countryCode ->
            when (countryCode) {
                "US", "GB", "CA" -> {
                    FieldType.values()
                        .filterNot { it == FieldType.PostalCode }
                        .map { it.identifierSpec }
                }
                else -> {
                    FieldType.values()
                        .map { it.identifierSpec }
                }
            }
        }
}
