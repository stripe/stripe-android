package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.BillingSectionElement
import com.stripe.android.paymentsheet.BillingSectionFieldRepository
import com.stripe.android.paymentsheet.FocusRequesterCount
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.SectionFieldElementType
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.GenericTextFieldConfig
import com.stripe.android.paymentsheet.elements.IdealBankConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec

/**
 * Transform a [LayoutSpec] data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through the field controller.
 */
internal fun transform(
    layout: LayoutSpec,
    merchantName: String,
    focusRequesterCount: FocusRequesterCount
) =
    layout.items.map {
        when (it) {
            is FormItemSpec.SaveForFutureUseSpec -> transform(it, merchantName)
            is FormItemSpec.SectionSpec -> transform(it, focusRequesterCount)
            is FormItemSpec.MandateTextSpec -> transform(it, merchantName)
            is FormItemSpec.BillingSectionSpec -> BillingSectionElement(
                IdentifierSpec("billing"),
                BillingSectionFieldRepository.INSTANCE
            )
        }
    }

private fun transform(
    spec: FormItemSpec.SectionSpec,
    focusRequesterCount: FocusRequesterCount
): FormElement.SectionElement {

    val fieldElements = transform(spec.fields, focusRequesterCount)

    // The controller of the section element will be the same as the field element
    // as there is only a single field in a section
    return FormElement.SectionElement(
        identifier = spec.identifier,
        fieldElements,
        SectionController(
            spec.title,
            fieldElements.map { it.controller }
        )
    )
}

internal fun transform(
    sectionFields: List<SectionFieldSpec>,
    focusRequesterCount: FocusRequesterCount
) = sectionFields.map {
    when (it) {
        is SectionFieldSpec.Email -> transform(
            it,
            focusRequesterCount
        )
        is SectionFieldSpec.Name -> transform(
            it,
            focusRequesterCount
        )
        is SectionFieldSpec.Country -> transform(
            it
        )
        is SectionFieldSpec.IdealBank -> transform(
            it
        )
        is SectionFieldSpec.GenericText -> transform(
            it,
            focusRequesterCount
        )
    }
}

private fun transform(
    spec: SectionFieldSpec.GenericText,
    focusRequesterCount: FocusRequesterCount
): SectionFieldElementType =
    SectionFieldElement.GenericText(
        spec.identifier,
        TextFieldController(
            GenericTextFieldConfig(
                label = spec.label,
            ),
            isRequired = spec.isRequired
        ),
        focusRequesterCount.getAndIncrement()
    )

private fun transform(spec: FormItemSpec.MandateTextSpec, merchantName: String) =
// It could be argued that the static text should have a controller, but
    // since it doesn't provide a form field we leave it out for now
    FormElement.MandateTextElement(
        spec.identifier,
        spec.stringResId,
        spec.color,
        merchantName
    )

private fun transform(
    spec: SectionFieldSpec.Name,
    focusRequesterCount: FocusRequesterCount
) =
    SectionFieldElement.Name(
        spec.identifier,
        TextFieldController(NameConfig()),
        focusRequesterCount.getAndIncrement()
    )

private fun transform(
    spec: SectionFieldSpec.Email,
    focusRequesterCount: FocusRequesterCount
) =
    SectionFieldElement.Email(
        spec.identifier,
        TextFieldController(EmailConfig()),
        focusRequesterCount.getAndIncrement()
    )

private fun transform(spec: SectionFieldSpec.Country) =
    SectionFieldElement.Country(
        spec.identifier,
        DropdownFieldController(CountryConfig(spec.onlyShowCountryCodes))
    )

private fun transform(spec: SectionFieldSpec.IdealBank) =
    SectionFieldElement.IdealBank(
        spec.identifier,
        DropdownFieldController(IdealBankConfig())
    )

private fun transform(spec: FormItemSpec.SaveForFutureUseSpec, merchantName: String) =
    FormElement.SaveForFutureUseElement(
        spec.identifier,
        SaveForFutureUseController(
            spec.identifierRequiredForFutureUse.map { element ->
                element.identifier
            }
        ),
        merchantName
    )
