package com.stripe.android.ui.core.forms

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AffirmTextSpec
import com.stripe.android.ui.core.elements.AfterpayClearpayTextSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.BacsDebitBankAccountSpec
import com.stripe.android.ui.core.elements.BacsDebitConfirmSpec
import com.stripe.android.ui.core.elements.BlikSpec
import com.stripe.android.ui.core.elements.BoletoTaxIdSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.CashAppPayMandateTextSpec
import com.stripe.android.ui.core.elements.ContactInformationSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.EmptyFormElement
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.KlarnaHeaderStaticTextSpec
import com.stripe.android.ui.core.elements.KonbiniConfirmationNumberSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SepaMandateTextSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.ui.core.elements.UpiSpec
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

/**
 * Transform a [LayoutSpec] data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through the field controller.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TransformSpecToElements(
    private val addressRepository: AddressRepository,
    private val initialValues: Map<IdentifierSpec, String?>,
    private val shippingValues: Map<IdentifierSpec, String?>?,
    private val amount: Amount?,
    private val saveForFutureUseInitialValue: Boolean,
    private val merchantName: String,
    private val context: Context,
    private val cbcEligibility: CardBrandChoiceEligibility
) {
    fun transform(list: List<FormItemSpec>): List<FormElement> =
        list.mapNotNull {
            when (it) {
                is SaveForFutureUseSpec -> it.transform(
                    saveForFutureUseInitialValue,
                    merchantName
                )
                is StaticTextSpec -> it.transform()
                is AfterpayClearpayTextSpec -> it.transform(requireNotNull(amount))
                is AffirmTextSpec -> it.transform()
                is EmptyFormSpec -> EmptyFormElement()
                is MandateTextSpec -> it.transform(merchantName)
                is AuBecsDebitMandateTextSpec -> it.transform(merchantName)
                is BacsDebitBankAccountSpec -> it.transform(initialValues)
                is BacsDebitConfirmSpec -> it.transform(merchantName, initialValues)
                is CardDetailsSectionSpec -> it.transform(
                    context = context,
                    cbcEligibility = cbcEligibility,
                    initialValues = initialValues
                )
                is BsbSpec -> it.transform(initialValues)
                is OTPSpec -> it.transform()
                is NameSpec -> it.transform(initialValues)
                is EmailSpec -> it.transform(initialValues)
                is PhoneSpec -> it.transform(initialValues)
                is SimpleTextSpec -> it.transform(initialValues)
                is AuBankAccountNumberSpec -> it.transform(initialValues)
                is IbanSpec -> it.transform(initialValues)
                is KlarnaHeaderStaticTextSpec -> it.transform()
                is KlarnaCountrySpec -> it.transform(amount?.currencyCode, initialValues)
                is DropdownSpec -> it.transform(initialValues)
                is CountrySpec -> it.transform(initialValues)
                is AddressSpec -> it.transform(
                    initialValues,
                    addressRepository,
                    shippingValues
                )
                is CardBillingSpec -> it.transform(
                    initialValues,
                    addressRepository,
                    shippingValues,
                )
                is BoletoTaxIdSpec -> it.transform(initialValues)
                is KonbiniConfirmationNumberSpec -> it.transform(initialValues)
                is SepaMandateTextSpec -> it.transform(merchantName)
                is UpiSpec -> it.transform()
                is BlikSpec -> it.transform()
                is ContactInformationSpec -> it.transform(initialValues)
                is PlaceholderSpec -> error("Placeholders should be processed before calling transform.")
                is CashAppPayMandateTextSpec -> it.transform(merchantName)
            }
        }.takeUnless { it.isEmpty() } ?: listOf(EmptyFormElement())
}
