package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.common.DropdownFieldController
import com.stripe.android.paymentsheet.elements.common.FocusRequesterCount
import com.stripe.android.paymentsheet.elements.common.FormElement
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import com.stripe.android.paymentsheet.specifications.FormElementSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec

/**
 * The purpose of this class is to transform a LayoutSpec data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through of the field controller.
 */
internal class TransformSpecToElement {
    fun transform(layout: LayoutSpec, focusRequesterCount: FocusRequesterCount) =
        layout.elements.map {
            when (it) {
                is FormElementSpec.SectionSpec -> transform(it, focusRequesterCount)
                is FormElementSpec.StaticTextSpec -> transform(it)
            }
        }

    private fun transform(
        spec: FormElementSpec.SectionSpec,
        focusRequesterCount: FocusRequesterCount
    ): FormElement.SectionElement {

        val fieldElement = when (spec.field) {
            FormElementSpec.SectionSpec.SectionFieldSpec.Email -> transform(
                spec.field as FormElementSpec.SectionSpec.SectionFieldSpec.Email,
                focusRequesterCount
            )
            FormElementSpec.SectionSpec.SectionFieldSpec.Name -> transform(
                spec.field as FormElementSpec.SectionSpec.SectionFieldSpec.Name,
                focusRequesterCount
            )
            FormElementSpec.SectionSpec.SectionFieldSpec.Country -> transform(
                spec.field as FormElementSpec.SectionSpec.SectionFieldSpec.Country
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

    private fun transform(spec: FormElementSpec.StaticTextSpec) =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        FormElement.StaticTextElement(
            spec.identifier,
            spec.stringResId,
            spec.color
        )

    private fun transform(
        spec: FormElementSpec.SectionSpec.SectionFieldSpec.Name,
        focusRequesterCount: FocusRequesterCount
    ) =
        FormElement.SectionElement.SectionFieldElement.Name(
            spec.identifier,
            TextFieldController(NameConfig()),
            focusRequesterCount.getAndIncrement()
        )


    private fun transform(
        spec: FormElementSpec.SectionSpec.SectionFieldSpec.Email,
        focusRequesterCount: FocusRequesterCount
    ) =
        FormElement.SectionElement.SectionFieldElement.Email(
            spec.identifier,
            TextFieldController(EmailConfig()),
            focusRequesterCount.getAndIncrement()
        )


    private fun transform(spec: FormElementSpec.SectionSpec.SectionFieldSpec.Country) =
        FormElement.SectionElement.SectionFieldElement.Country(
            spec.identifier,
            DropdownFieldController(CountryConfig())
        )

}