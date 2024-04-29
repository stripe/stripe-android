package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.uicore.address.CountryAddressSchema
import com.stripe.android.uicore.address.FieldSchema
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.address.NameType
import com.stripe.android.uicore.address.transformToElementList
import com.stripe.android.uicore.elements.AddressController
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.utils.stateFlowOf

internal fun createAddressController(
    fillAddress: Boolean = false,
): AddressController {
    val elements = listOf(
        CountryAddressSchema(
            type = FieldType.AddressLine1,
            required = true,
        ),
        CountryAddressSchema(
            type = FieldType.AddressLine2,
            required = false,
        ),
        CountryAddressSchema(
            type = FieldType.Locality,
            required = true,
            schema = FieldSchema(nameType = NameType.City),
        ),
        CountryAddressSchema(
            type = FieldType.PostalCode,
            required = true,
            schema = FieldSchema(
                isNumeric = true,
                nameType = NameType.Zip,
            ),
        ),
        CountryAddressSchema(
            type = FieldType.AdministrativeArea,
            required = true,
            schema = FieldSchema(nameType = NameType.State),
        )
    ).transformToElementList(countryCode = "US")

    if (fillAddress) {
        elements.forEach { element ->
            if (element is RowElement) {
                element.fields.forEach(::setAddressValueForElement)
            } else {
                setAddressValueForElement(element)
            }
        }
    }

    return AddressController(
        stateFlowOf(
            listOf(
                CountryElement(
                    identifier = IdentifierSpec.BillingAddress,
                    controller = DropdownFieldController(
                        config = CountryConfig(setOf("US", "CA")),
                        initialValue = "US"
                    )
                ),
            ).plus(elements)
        )
    )
}

private fun setAddressValueForElement(element: SectionFieldElement) {
    val identifier = element.identifier

    element.setRawValue(mapOf(identifier to identifier.toTestAddressValue()))
}

private fun IdentifierSpec.toTestAddressValue(): String {
    return when (this) {
        FieldType.AddressLine1.identifierSpec -> "354 Oyster Point Blvd"
        FieldType.AddressLine2.identifierSpec -> "Levels 1-5"
        FieldType.Locality.identifierSpec -> "South San Francisco"
        FieldType.AdministrativeArea.identifierSpec -> "CA"
        FieldType.PostalCode.identifierSpec -> "94080"
        else -> ""
    }
}
