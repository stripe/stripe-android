package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.Identifier
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.IbanConfig
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.SimpleDropdownConfig
import com.stripe.android.paymentsheet.elements.SimpleTextFieldConfig
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.getValue
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.ResourceRepository
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec

/**
 * Transform a [LayoutSpec] data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through the field controller.
 */
internal class TransformSpecToElement(
    private val resourceRepository: ResourceRepository,
    private val initialValues: ComposeFragmentArguments
) {
    internal fun transform(
        list: List<FormItemSpec>
    ): List<FormElement> =
        list.map {
            when (it) {
                is FormItemSpec.SaveForFutureUseSpec -> it.transform(initialValues.merchantName)
                is FormItemSpec.SectionSpec -> it.transform(initialValues)
                is FormItemSpec.MandateTextSpec -> it.transform(initialValues.merchantName)
            }
        }

    private fun FormItemSpec.SectionSpec.transform(initialValues: ComposeFragmentArguments): FormElement.SectionElement {
        val fieldElements = this.fields.transform(initialValues)

        // The controller of the section element will be the same as the field element
        // as there is only a single field in a section
        return FormElement.SectionElement(
            Identifier.fromSpec(this.identifier),
            fieldElements,
            SectionController(
                this.title,
                fieldElements.map { it.controller }
            )
        )
    }

    /**
     * This function will transform a list of specs into a list of elements
     */
    private fun List<SectionFieldSpec>.transform(initialValues: ComposeFragmentArguments) =
        this.map {
            when (it) {
                is SectionFieldSpec.Email -> it.transform(initialValues.billingDetails?.email)
                is SectionFieldSpec.Iban -> it.transform()
                is SectionFieldSpec.Country -> it.transform(initialValues.billingDetails?.address?.country)
                is SectionFieldSpec.BankDropdown -> it.transform()
                is SectionFieldSpec.SimpleText -> it.transform(initialValues)
                is SectionFieldSpec.AddressSpec -> transformAddress(initialValues)
            }
        }

    private fun transformAddress(initialValues: ComposeFragmentArguments?) =
        SectionFieldElement.AddressElement(
            Identifier.Generic("billing"),
            resourceRepository.addressRepository,
            initialValues
        )

    private fun FormItemSpec.MandateTextSpec.transform(merchantName: String) =
// It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        FormElement.MandateTextElement(
            Identifier.fromSpec(this.identifier),
            this.stringResId,
            this.color,
            merchantName
        )

    private fun SectionFieldSpec.Email.transform(email: String?) =
        SectionFieldElement.Email(
            Identifier.fromSpec(this.identifier),
            TextFieldController(EmailConfig(), initialValue = email),
        )

    private fun SectionFieldSpec.Iban.transform() =
        SectionFieldElement.Iban(
            Identifier.fromSpec(this.identifier),
            TextFieldController(IbanConfig())
        )

    private fun SectionFieldSpec.Country.transform(country: String?) =
        SectionFieldElement.Country(
            Identifier.fromSpec(this.identifier),
            DropdownFieldController(CountryConfig(this.onlyShowCountryCodes), country)
        )

    private fun SectionFieldSpec.BankDropdown.transform() =
        SectionFieldElement.SimpleDropdown(
            Identifier.fromSpec(this.identifier),
            DropdownFieldController(
                SimpleDropdownConfig(
                    label,
                    resourceRepository.bankRepository.get(this.bankType)
                )
            )
        )

    private fun FormItemSpec.SaveForFutureUseSpec.transform(merchantName: String) =
        FormElement.SaveForFutureUseElement(
            Identifier.fromSpec(this.identifier),
            SaveForFutureUseController(
                this.identifierRequiredForFutureUse.map { requiredItemSpec ->
                    Identifier.fromSpec(requiredItemSpec.identifier)
                }
            ),
            merchantName
        )
}

internal fun SectionFieldSpec.SimpleText.transform(initialValues: ComposeFragmentArguments? = null): SectionFieldElement =
    SectionFieldElement.SimpleText(
        Identifier.fromSpec(this.identifier),
        TextFieldController(
            SimpleTextFieldConfig(
                label = this.label,
                capitalization = this.capitalization,
                keyboard = this.keyboardType
            ),
            initialValue = initialValues?.getValue(Identifier.fromSpec(this.identifier)),
            showOptionalLabel = this.showOptionalLabel
        )
    )
