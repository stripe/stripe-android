package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class AddressTextFieldElement(
    override val identifier: IdentifierSpec,
    val country: String?,
    googlePlacesApiKey: String?,
    config: TextFieldConfig,
    onClick: () -> Unit
) : SectionSingleFieldElement(identifier) {

    override val controller: AddressTextFieldController =
        AddressTextFieldController(
            country,
            googlePlacesApiKey,
            config,
            onClick
        )

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.address.map { address ->
            listOf(
                IdentifierSpec.Country to FormFieldEntry(address?.country, true),
                IdentifierSpec.City to FormFieldEntry(address?.city, true),
                IdentifierSpec.Line1 to FormFieldEntry(address?.line1, true),
                IdentifierSpec.Line2 to FormFieldEntry(address?.line2, true),
                IdentifierSpec.State to FormFieldEntry(address?.state, true),
                IdentifierSpec.PostalCode to FormFieldEntry(address?.postalCode, true)
            )
        }
    }

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        rawValuesMap[IdentifierSpec.Country]?.let {
            controller.onRawValueChange(it)
        }
    }

    override fun getTextFieldIdentifiers(): Flow<List<IdentifierSpec>> =
        MutableStateFlow(
            listOf(
                IdentifierSpec.Country,
                IdentifierSpec.City,
                IdentifierSpec.Line1,
                IdentifierSpec.Line2,
                IdentifierSpec.State,
                IdentifierSpec.PostalCode
            )
        )
}