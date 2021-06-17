package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.common.DropdownFieldController
import com.stripe.android.paymentsheet.elements.common.FocusRequesterCount
import com.stripe.android.paymentsheet.elements.common.FormElement
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import com.stripe.android.paymentsheet.specification.FormElementSpec
import com.stripe.android.paymentsheet.specification.LayoutSpec

internal class TransformSpecToElement {
    fun createElement(layout: LayoutSpec, focusRequesterCount: FocusRequesterCount) =
        layout.elements.map {
            when (it) {
                is FormElementSpec.SectionSpec -> createElement(it, focusRequesterCount)
                is FormElementSpec.StaticSpec.TextSpec -> createElement(it)
            }
        }

    private fun createElement(
        spec: FormElementSpec.SectionSpec,
        focusRequesterCount: FocusRequesterCount
    ): FormElement.SectionElement {

        val fieldElement = when (spec.field) {
            FormElementSpec.SectionSpec.SectionFieldSpec.Email -> createElement(
                spec.field as FormElementSpec.SectionSpec.SectionFieldSpec.Email,
                focusRequesterCount
            )
            FormElementSpec.SectionSpec.SectionFieldSpec.Name -> createElement(
                spec.field as FormElementSpec.SectionSpec.SectionFieldSpec.Name,
                focusRequesterCount
            )
            FormElementSpec.SectionSpec.SectionFieldSpec.Country -> createElement(spec.field as FormElementSpec.SectionSpec.SectionFieldSpec.Country)
        }

        return FormElement.SectionElement(
            identifier = spec.identifier,
            fieldElement,
            fieldElement.controller
        )
    }

    private fun createElement(spec: FormElementSpec.StaticSpec.TextSpec) =
        FormElement.StaticElement(
            spec.identifier,
            spec.stringResId,
            spec.color
        )

    private fun createElement(
        spec: FormElementSpec.SectionSpec.SectionFieldSpec.Name,
        focusRequesterCount: FocusRequesterCount
    ) =
        FormElement.SectionElement.SectionFieldElement.Name(
            spec.identifier,
            TextFieldController(NameConfig()),
            focusRequesterCount.getAndIncrement()
        )


    private fun createElement(
        spec: FormElementSpec.SectionSpec.SectionFieldSpec.Email,
        focusRequesterCount: FocusRequesterCount
    ) =
        FormElement.SectionElement.SectionFieldElement.Email(
            spec.identifier,
            TextFieldController(EmailConfig()),
            focusRequesterCount.getAndIncrement()
        )


    private fun createElement(spec: FormElementSpec.SectionSpec.SectionFieldSpec.Country) =
        FormElement.SectionElement.SectionFieldElement.Country(
            spec.identifier,
            DropdownFieldController(CountryConfig())
        )

}