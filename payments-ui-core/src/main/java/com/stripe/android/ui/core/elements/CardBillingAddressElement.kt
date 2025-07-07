package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressFieldsElement
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
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
    override val identifier: IdentifierSpec,
    rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    ),
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?,
    private val collectionMode: BillingDetailsCollectionConfiguration.AddressCollectionMode =
        BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
) : AddressFieldsElement {
    private val addressElement = autocompleteAddressInteractorFactory?.takeIf {
        collectionMode == BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
    }?.let { factory ->
        AutocompleteAddressElement(
            identifier = identifier,
            initialValues = rawValuesMap,
            countryCodes = countryCodes,
            countryDropdownFieldController = countryDropdownFieldController,
            interactorFactory = factory,
            shippingValuesMap = shippingValuesMap,
            sameAsShippingElement = sameAsShippingElement,
        )
    } ?: run {
        AddressElement(
            _identifier = identifier,
            rawValuesMap = rawValuesMap,
            countryCodes = countryCodes,
            addressInputMode = AddressInputMode.NoAutocomplete(),
            countryElement = CountryElement(
                identifier = IdentifierSpec.Country,
                controller = countryDropdownFieldController,
            ),
            shippingValuesMap = shippingValuesMap,
            sameAsShippingElement = sameAsShippingElement,
        )
    }

    override val countryElement: CountryElement = addressElement.countryElement

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

    override val allowsUserInteraction: Boolean = addressElement.allowsUserInteraction

    override val mandateText: ResolvableString? = addressElement.mandateText

    override fun getFormFieldValueFlow() = addressElement.getFormFieldValueFlow()

    override fun sectionFieldErrorController() = addressElement.sectionFieldErrorController()

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) = addressElement.setRawValue(rawValuesMap)

    override fun getTextFieldIdentifiers() = addressElement.getTextFieldIdentifiers()
}
