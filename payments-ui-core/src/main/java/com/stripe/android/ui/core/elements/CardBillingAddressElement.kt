package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressType
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This is a special type of AddressElement that
 * removes fields from the address based on the country.  It
 * is only intended to be used with the card payment method.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardBillingAddressElement(
    identifier: IdentifierSpec,
    rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
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
    rawValuesMap,
    AddressType.Normal(),
    countryCodes,
    countryDropdownFieldController,
    sameAsShippingElement,
    shippingValuesMap
) {
    // Save for future use puts this in the controller rather than element
    // card and achv2 uses save for future use
    val hiddenIdentifiers: StateFlow<Set<IdentifierSpec>> =
        countryDropdownFieldController.rawFieldValue.mapAsStateFlow { countryCode ->
            when (collectionMode) {
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                    FieldType.entries
                        .map { it.identifierSpec }
                        .toSet()
                }
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                    emptySet()
                }
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
                    when (countryCode) {
                        "US", "GB", "CA" -> {
                            FieldType.entries
                                // Filtering name causes the field to be hidden even outside
                                // of this form.
                                .filterNot { it == FieldType.PostalCode || it == FieldType.Name }
                                .map { it.identifierSpec }
                                .toSet()
                        }
                        else -> {
                            FieldType.entries
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
