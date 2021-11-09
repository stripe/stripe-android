package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.AddressElement
import com.stripe.android.paymentsheet.elements.AddressSpec
import com.stripe.android.paymentsheet.elements.AfterpayClearpayElement
import com.stripe.android.paymentsheet.elements.AfterpayClearpaySpec
import com.stripe.android.paymentsheet.elements.BankDropdownSpec
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.CountryElement
import com.stripe.android.paymentsheet.elements.CountrySpec
import com.stripe.android.paymentsheet.elements.CardBillingElement
import com.stripe.android.paymentsheet.elements.CardBillingSpec
import com.stripe.android.paymentsheet.elements.CardDetailsElement
import com.stripe.android.paymentsheet.elements.CardDetailsSpec
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
import com.stripe.android.paymentsheet.elements.KlarnaCountrySpec
import com.stripe.android.paymentsheet.elements.KlarnaHelper
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.ResourceRepository
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
import com.stripe.android.paymentsheet.elements.SimpleTextFieldController
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.StaticTextElement
import com.stripe.android.paymentsheet.elements.StaticTextSpec
import com.stripe.android.paymentsheet.elements.TextFieldController
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
    internal fun transform(
        list: List<FormItemSpec>
    ): List<FormElement> =
        list.map {
            when (it) {
                is SaveForFutureUseSpec -> it.transform(initialValues)
                is SectionSpec -> it.transform(initialValues)
                is StaticTextSpec -> it.transform(initialValues.merchantName)
                is AfterpayClearpaySpec ->
                    it.transform(requireNotNull(initialValues.amount))
            }
        }

    private fun SectionSpec.transform(
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
    private fun List<SectionFieldSpec>.transform(initialValues: FormFragmentArguments) =
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
                is KlarnaCountrySpec -> it.transform(
                    initialValues.amount?.currencyCode,
                    initialValues.billingDetails?.address?.country
                )
                is CardDetailsSpec -> transformCreditDetail()
                is CardBillingSpec -> transformCreditBilling()
            }
        }

    private fun transformAddress(initialValues: FormFragmentArguments) =
        AddressElement(
            IdentifierSpec.Generic("billing"),
            resourceRepository.addressRepository,
            initialValues
        )

    private fun transformCreditDetail() = CardDetailsElement(
        IdentifierSpec.Generic("credit_detail")
    )

    private fun transformCreditBilling() = CardBillingElement(
        IdentifierSpec.Generic("credit_billing"),
        resourceRepository.addressRepository
    )

    private fun StaticTextSpec.transform(merchantName: String) =
        /**
         * It could be argued that the static text should have a controller, but
         * since it doesn't provide a form field we leave it out for now
         */
        StaticTextElement(
            this.identifier,
            this.stringResId,
            this.color,
            merchantName,
            this.fontSizeSp,
            this.letterSpacingSp
        )

    private fun EmailSpec.transform(email: String?) =
        EmailElement(
            this.identifier,
            SimpleTextFieldController(EmailConfig(), initialValue = email),
        )

    private fun IbanSpec.transform() =
        IbanElement(
            this.identifier,
            SimpleTextFieldController(IbanConfig())
        )

    private fun CountrySpec.transform(country: String?) =
        CountryElement(
            this.identifier,
            DropdownFieldController(CountryConfig(this.onlyShowCountryCodes), country)
        )

    private fun KlarnaCountrySpec.transform(currencyCode: String?, country: String?) =
        CountryElement(
            this.identifier,
            DropdownFieldController(
                CountryConfig(KlarnaHelper.getAllowedCountriesForCurrency(currencyCode)), country
            )
        )

    private fun BankDropdownSpec.transform() =
        SimpleDropdownElement(
            this.identifier,
            DropdownFieldController(
                SimpleDropdownConfig(
                    label,
                    resourceRepository.bankRepository.get(this.bankType)
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

    private fun AfterpayClearpaySpec.transform(amount: Amount) =
        AfterpayClearpayElement(this.identifier, amount)
}

internal fun SimpleTextSpec.transform(
    initialValues: FormFragmentArguments? = null
): SectionSingleFieldElement =
    SimpleTextElement(
        this.identifier,
        SimpleTextFieldController(
            SimpleTextFieldConfig(
                label = this.label,
                capitalization = this.capitalization,
                keyboard = this.keyboardType
            ),
            initialValue = initialValues?.getValue(this.identifier),
            showOptionalLabel = this.showOptionalLabel
        )
    )
