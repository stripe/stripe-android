package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.FocusRequesterCount
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.common.DropdownFieldController
import com.stripe.android.paymentsheet.elements.common.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec

/**
 * The purpose of this class is to transform a LayoutSpec data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through of the field controller.
 */
internal class TransformSpecToElement {
    fun transform(layout: LayoutSpec, focusRequesterCount: FocusRequesterCount) =
        layout.items.map {
            when (it) {
                is FormItemSpec.SaveForFutureUseSpec -> transform(it)
                is FormItemSpec.SectionSpec -> transform(it, focusRequesterCount)
                is FormItemSpec.StaticTextSpec -> transform(it)
            }
        }

    private fun transform(
        spec: FormItemSpec.SectionSpec,
        focusRequesterCount: FocusRequesterCount
    ): FormElement.SectionElement {

        val fieldElement = when (spec.field) {
            SectionFieldSpec.Email -> transform(
                spec.field as SectionFieldSpec.Email,
                focusRequesterCount
            )
            SectionFieldSpec.Name -> transform(
                spec.field as SectionFieldSpec.Name,
                focusRequesterCount
            )
            SectionFieldSpec.Country -> transform(
                spec.field as SectionFieldSpec.Country
            )
        }

        // The controller of the section element will be the same as the field element
        // as there is only a single field in a section
        return FormElement.SectionElement(
            identifier = spec.identifier,
            fieldElement,
            fieldElement.controller
        )
    }

    private fun transform(spec: FormItemSpec.StaticTextSpec) =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        FormElement.StaticTextElement(
            spec.identifier,
            spec.stringResId,
            spec.color
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
            DropdownFieldController(CountryConfig())
        )

    private fun transform(spec: FormItemSpec.SaveForFutureUseSpec) =
        FormElement.SaveForFutureUseElement(
            spec.identifier,
            SaveForFutureUseController(
                spec.identifierRequiredForFutureUse.map { element ->
                    element.identifier
                }
            )
        )
}
