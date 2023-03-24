package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
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
    shippingValuesMap: Map<IdentifierSpec, String?>?,
    private val collectionMode: BillingDetailsCollectionConfiguration.AddressCollectionMode =
        BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
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
            when (collectionMode) {
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                    FieldType.values()
                        .map { it.identifierSpec }
                        .toSet()
                }
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                    emptySet()
                }
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
                    when (countryCode) {
                        "US", "GB", "CA" -> {
                            FieldType.values()
                                // Filtering name causes the field to be hidden even outside
                                // of this form.
                                .filterNot { it == FieldType.PostalCode || it == FieldType.Name }
                                .map { it.identifierSpec }
                                .toSet()
                        }
                        else -> {
                            FieldType.values()
                                // Filtering name causes the field to be hidden even outside
                                // of this form.
                                .filterNot { it == FieldType.Name }
                                .map { it.identifierSpec }
                                .toSet()
                        }
                    }
                }
            }
        }
}
