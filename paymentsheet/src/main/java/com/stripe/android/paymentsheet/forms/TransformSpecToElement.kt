package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.AddressElement
import com.stripe.android.paymentsheet.elements.AddressSpec
import com.stripe.android.paymentsheet.elements.AfterpayClearpayHeaderElement
import com.stripe.android.paymentsheet.elements.AfterpayClearpayTextSpec
import com.stripe.android.paymentsheet.elements.BankDropdownSpec
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.CountryElement
import com.stripe.android.paymentsheet.elements.CountrySpec
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.EmailElement
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.FormElement
import com.stripe.android.paymentsheet.elements.FormItemSpec
import com.stripe.android.paymentsheet.elements.IbanConfig
import com.stripe.android.paymentsheet.elements.IbanElement
import com.stripe.android.paymentsheet.elements.IbanSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.MandateTextElement
import com.stripe.android.paymentsheet.elements.MandateTextSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SaveForFutureUseElement
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.SectionElement
import com.stripe.android.paymentsheet.elements.SectionFieldSpec
import com.stripe.android.paymentsheet.elements.SectionSingleFieldElement
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleDropdownConfig
import com.stripe.android.paymentsheet.elements.SimpleDropdownElement
import com.stripe.android.paymentsheet.elements.SimpleTextElement
import com.stripe.android.paymentsheet.elements.SimpleTextFieldConfig
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.forms.resources.ResourceRepository
import com.stripe.android.paymentsheet.model.Amount
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.getValue
import javax.inject.Inject

/**
 * Transform a [LayoutSpec] data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through the field controller.
 */
internal class TransformSpecToElement @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val initialValues: FormFragmentArguments
) {
    internal suspend fun transform(
        list: List<FormItemSpec>
    ): List<FormElement> =
        list.map {
            when (it) {
                is SaveForFutureUseSpec -> it.transform(initialValues)
                is SectionSpec -> it.transform(initialValues)
                is MandateTextSpec -> it.transform(initialValues.merchantName)
                is AfterpayClearpayTextSpec ->
                    it.transform(requireNotNull(initialValues.amount))
            }
        }

    private suspend fun SectionSpec.transform(
        initialValues: FormFragmentArguments
    ): SectionElement {
        val fieldElements = this.fields.transform(initialValues)

        // The controller of the section element will be the same as the field element
        // as there is only a single field in a section
        return SectionElement(
            this.identifier,
            fieldElements,
            SectionController(
                this.title,
                fieldElements.map { it.sectionFieldErrorController() }
            )
        )
    }

    /**
     * This function will transform a list of specs into a list of elements
     */
    private suspend fun List<SectionFieldSpec>.transform(initialValues: FormFragmentArguments) =
        this.map {
            when (it) {
                is EmailSpec -> it.transform(initialValues.billingDetails?.email)
                is IbanSpec -> it.transform()
                is BankDropdownSpec -> it.transform()
                is SimpleTextSpec -> it.transform(initialValues)
                is AddressSpec -> transformAddress(initialValues)
                is CountrySpec -> it.transform(
                    initialValues.billingDetails?.address?.country
                )
            }
        }

    private suspend fun transformAddress(initialValues: FormFragmentArguments) =
        AddressElement(
            IdentifierSpec.Generic("billing"),
            resourceRepository.getAddressRepository(),
            initialValues
        )

    private fun MandateTextSpec.transform(merchantName: String) =
// It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        MandateTextElement(
            this.identifier,
            this.stringResId,
            this.color,
            merchantName
        )

    private fun EmailSpec.transform(email: String?) =
        EmailElement(
            this.identifier,
            TextFieldController(EmailConfig(), initialValue = email),
        )

    private fun IbanSpec.transform() =
        IbanElement(
            this.identifier,
            TextFieldController(IbanConfig())
        )

    private fun CountrySpec.transform(country: String?) =
        CountryElement(
            this.identifier,
            DropdownFieldController(CountryConfig(this.onlyShowCountryCodes), country)
        )

    private suspend fun BankDropdownSpec.transform() =
        SimpleDropdownElement(
            this.identifier,
            DropdownFieldController(
                SimpleDropdownConfig(
                    label,
                    resourceRepository.getBankRepository().get(this.bankType)
                )
            )
        )

    private fun SaveForFutureUseSpec.transform(initialValues: FormFragmentArguments? = null) =
        SaveForFutureUseElement(
            this.identifier,
            SaveForFutureUseController(
                this.identifierRequiredForFutureUse.map { requiredItemSpec ->
                    requiredItemSpec.identifier
                },
                initialValues?.showCheckboxControlledFields != false
            ),
            initialValues?.merchantName
        )

    private fun AfterpayClearpayTextSpec.transform(amount: Amount) =
        AfterpayClearpayHeaderElement(this.identifier, amount)
}

internal fun SimpleTextSpec.transform(
    initialValues: FormFragmentArguments? = null
): SectionSingleFieldElement =
    SimpleTextElement(
        this.identifier,
        TextFieldController(
            SimpleTextFieldConfig(
                label = this.label,
                capitalization = this.capitalization,
                keyboard = this.keyboardType
            ),
            initialValue = initialValues?.getValue(this.identifier),
            showOptionalLabel = this.showOptionalLabel
        )
    )
