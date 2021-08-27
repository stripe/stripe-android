package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.address.FieldType
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


/**
 * This is a special type of AddressElement that
 * removes fields from the address based on the country.  It
 * is only intended to be used with the credit payment method.
 */
internal class CreditBillingElement(
    identifier: IdentifierSpec,
    addressFieldRepository: AddressFieldElementRepository,
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes)
    ),
) : SectionMultiFieldElement.AddressElement(
    identifier,
    addressFieldRepository,
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
