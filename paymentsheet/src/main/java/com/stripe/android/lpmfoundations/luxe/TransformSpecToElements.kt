package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specsForConfiguration
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AffirmTextSpec
import com.stripe.android.ui.core.elements.AfterpayClearpayTextSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.BacsDebitBankAccountSpec
import com.stripe.android.ui.core.elements.BacsDebitConfirmSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.CashAppPayMandateTextSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.EmptyFormElement
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.KlarnaHeaderStaticTextSpec
import com.stripe.android.ui.core.elements.KlarnaMandateTextSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.SepaMandateTextSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

/**
 * Transform a [LayoutSpec] data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through the field controller.
 *
 */
internal class TransformSpecToElements(
    private val arguments: UiDefinitionFactory.Arguments,
) {
    fun transform(
        specs: List<FormItemSpec>,
        placeholderOverrideList: List<IdentifierSpec> = emptyList(),
    ): List<FormElement> {
        return specsForConfiguration(
            configuration = arguments.billingDetailsCollectionConfiguration,
            placeholderOverrideList = placeholderOverrideList,
            requiresMandate = arguments.requiresMandate,
            specs = specs,
        ).flatMap { spec ->
            when (spec) {
                is StaticTextSpec -> listOf(spec.transform())
                is AfterpayClearpayTextSpec -> listOf(spec.transform())
                is AffirmTextSpec -> listOf(spec.transform())
                is EmptyFormSpec -> listOf(EmptyFormElement())
                is MandateTextSpec -> listOf(spec.transform(arguments.merchantName))
                is AuBecsDebitMandateTextSpec -> listOf(spec.transform(arguments.merchantName))
                is BacsDebitBankAccountSpec -> listOf(spec.transform(arguments.initialValues))
                is BacsDebitConfirmSpec -> listOf(spec.transform(arguments.merchantName, arguments.initialValues))
                is BsbSpec -> listOf(spec.transform(arguments.initialValues))
                is OTPSpec -> listOf(spec.transform())
                is NameSpec -> listOf(spec.transform(arguments.initialValues))
                is EmailSpec -> listOf(spec.transform(arguments.initialValues))
                is PhoneSpec -> listOf(spec.transform(arguments.initialValues))
                is SimpleTextSpec -> listOf(spec.transform(arguments.initialValues))
                is AuBankAccountNumberSpec -> listOf(spec.transform(arguments.initialValues))
                is IbanSpec -> listOf(spec.transform(arguments.initialValues))
                is KlarnaHeaderStaticTextSpec -> listOf(spec.transform())
                is DropdownSpec -> listOf(spec.transform(arguments.initialValues))
                is CountrySpec -> listOf(spec.transform(arguments.initialValues))
                is AddressSpec -> spec.transform(arguments.initialValues, arguments.shippingValues)
                is SepaMandateTextSpec -> listOf(spec.transform(arguments.merchantName))
                is PlaceholderSpec -> listOf() // Placeholders should be processed before calling transform.
                is CashAppPayMandateTextSpec -> listOf(spec.transform(arguments.merchantName))
                is KlarnaMandateTextSpec -> listOf(spec.transform(arguments.merchantName))
            }
        }
    }
}
