package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AutocompleteAddressTextFieldElement(
    override val identifier: IdentifierSpec,
    country: String,
    googlePlacesApiKey: String
) : SectionSingleFieldElement(identifier) {

    override val controller: AutocompleteAddressTextFieldController =
        AutocompleteAddressTextFieldController(
            country,
            googlePlacesApiKey
        )

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.address.map { address ->
            listOf(
                IdentifierSpec.Country to FormFieldEntry(address?.country, true),
                IdentifierSpec.City to FormFieldEntry(address?.city, true),
                IdentifierSpec.Line1 to FormFieldEntry(address?.line1, true),
                IdentifierSpec.State to FormFieldEntry(address?.state, true),
                IdentifierSpec.PostalCode to FormFieldEntry(address?.postalCode, true)
            )
        }
    }

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        rawValuesMap[identifier]?.let { controller.onRawValueChange(it) }
    }

    override fun getTextFieldIdentifiers(): Flow<List<IdentifierSpec>> =
        MutableStateFlow(
            listOf(
                IdentifierSpec.Country,
                IdentifierSpec.City,
                IdentifierSpec.Line1,
                IdentifierSpec.State,
                IdentifierSpec.PostalCode
            )
        )
}