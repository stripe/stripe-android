package com.stripe.android.ui.core.forms

import android.content.Context
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AffirmTextSpec
import com.stripe.android.ui.core.elements.AfterpayClearpayTextSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.BankRepository
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.EmptyFormElement
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionController
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionFieldSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.ui.core.forms.resources.ResourceRepository

/**
 * Transform a [LayoutSpec] data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through the field controller.
 */
class TransformSpecToElements(
    private val resourceRepository: ResourceRepository,
    private val initialValues: Map<IdentifierSpec, String?>,
    private val amount: Amount?,
    private val saveForFutureUseInitialValue: Boolean,
    private val merchantName: String,
    private val context: Context
) {
    fun transform(
        list: List<FormItemSpec>,
    ): List<FormElement> =
        list.map {
            when (it) {
                is SaveForFutureUseSpec -> it.transform(
                    saveForFutureUseInitialValue,
                    merchantName
                )
                is SectionSpec -> it.transform(initialValues)
                is StaticTextSpec -> it.transform()
                is MandateTextSpec -> it.transform(merchantName)
                is AfterpayClearpayTextSpec ->
                    it.transform(requireNotNull(amount))
                is AffirmTextSpec ->
                    it.transform()
                is EmptyFormSpec -> EmptyFormElement()
                is AuBecsDebitMandateTextSpec -> it.transform(merchantName)
                is CardDetailsSectionSpec -> it.transform(context, initialValues)
                is BsbSpec -> it.transform(initialValues)
                is EmailSpec -> it.transform(initialValues)
                is NameSpec -> it.transform(initialValues)
                is AuBankAccountNumberSpec -> it.transform(initialValues)
                is IbanSpec -> it.transform(initialValues)
                is KlarnaCountrySpec -> it.transform(
                    amount?.currencyCode,
                    initialValues
                )
                is CountrySpec -> it.transform(
                    initialValues
                )
                is CardBillingSpec -> it.transform(
                    resourceRepository.getAddressRepository(),
                    initialValues
                )
                is AddressSpec -> it.transform(
                    initialValues,
                    resourceRepository.getAddressRepository()
                )
                is BankDropdownSpec -> it.transform(
                    resourceRepository.getBankRepository(),
                    initialValues[it.identifier]
                )
            }
        }

    private fun SectionSpec.transform(
        initialValues: Map<IdentifierSpec, String?>
    ): SectionElement {
        val fieldElements = this.fields.transform(
            initialValues
        )

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
    private fun List<SectionFieldSpec>.transform(
        initialValues: Map<IdentifierSpec, String?>
    ) =
        this.map {
            when (it) {
                is SimpleTextSpec -> it.transform(initialValues)
            }
        }
}
