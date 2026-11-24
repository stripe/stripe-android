package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.compose.ui.autofill.ContentType
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.address.CountryAddressSchema
import com.stripe.android.uicore.address.FieldSchema
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.address.NameType
import com.stripe.android.uicore.address.transformToElementList
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionFieldElement

internal fun createAddressSection(
    formArgs: FormArguments,
    fillAddress: Boolean = false,
): SectionElement? {
    if (formArgs.billingDetailsCollectionConfiguration.address != AddressCollectionMode.Full) return null

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

    return SectionElement.wrap(
        sectionFieldElements = listOf(
            CountryElement(
                identifier = FormFieldId.BillingAddress,
                controller = DropdownFieldController(
                    config = CountryConfig(setOf("US", "CA"), autofillType = ContentType.AddressCountry),
                    initialValue = "US"
                )
            ),
        ) + elements,
        label = resolvableString(R.string.stripe_billing_details),
    )
}

private fun setAddressValueForElement(element: SectionFieldElement) {
    val identifier = element.identifier

    element.setRawValue(mapOf(identifier to identifier.toTestAddressValue()))
}

private fun FormFieldId.toTestAddressValue(): String {
    return when (this) {
        FieldType.AddressLine1.formFieldId -> "354 Oyster Point Blvd"
        FieldType.AddressLine2.formFieldId -> "Levels 1-5"
        FieldType.Locality.formFieldId -> "South San Francisco"
        FieldType.AdministrativeArea.formFieldId -> "CA"
        FieldType.PostalCode.formFieldId -> "94080"
        else -> ""
    }
}
