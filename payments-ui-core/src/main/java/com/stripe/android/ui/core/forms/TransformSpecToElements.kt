package com.stripe.android.ui.core.forms

import android.content.Context
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AffirmTextSpec
import com.stripe.android.ui.core.elements.AfterpayClearpayTextSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.EmptyFormElement
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.KlarnaHeaderStaticTextSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
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
        list: List<FormItemSpec>
    ): List<FormElement> =
        list.map {
            when (it) {
                is SaveForFutureUseSpec -> it.transform(
                    saveForFutureUseInitialValue,
                    merchantName
                )
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
                is OTPSpec -> it.transform()
                is EmailSpec -> it.transform(initialValues)
                is NameSpec -> it.transform(initialValues)
                is AuBankAccountNumberSpec -> it.transform(initialValues)
                is IbanSpec -> it.transform(initialValues)
                is KlarnaCountrySpec -> it.transform(
                    amount?.currencyCode,
                    initialValues
                )
                is DropdownSpec -> it.transform(initialValues)
                is SimpleTextSpec -> it.transform(initialValues)
                is AddressSpec -> it.transform(
                    initialValues,
                    resourceRepository.getAddressRepository()
                )
                is CountrySpec -> it.transform(
                    initialValues
                )
                is CardBillingSpec -> it.transform(
                    resourceRepository.getAddressRepository(),
                    initialValues
                )
                is KlarnaHeaderStaticTextSpec -> it.transform()
            }
        }
}
