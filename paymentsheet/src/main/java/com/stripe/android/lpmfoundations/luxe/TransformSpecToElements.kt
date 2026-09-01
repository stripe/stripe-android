package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specsForConfiguration
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

/**
 * Transform form item specs into elements with controllers and identifiers.
 *
 */
internal class TransformSpecToElements(
    private val arguments: UiDefinitionFactory.Arguments,
) {
    fun transform(
        specs: List<FormItemSpec>,
        placeholderOverrideList: List<IdentifierSpec> = emptyList(),
        termsDisplay: PaymentSheet.TermsDisplay,
    ): List<FormElement> {
        return specsForConfiguration(
            configuration = arguments.billingDetailsCollectionConfiguration,
            placeholderOverrideList = placeholderOverrideList,
            specs = specs,
            termsDisplay = termsDisplay,
        ).flatMap { spec ->
            when (spec) {
                is NameSpec -> listOf(spec.transform(arguments.initialValues))
                is EmailSpec -> listOf(spec.transform(arguments.initialValues))
                is PhoneSpec -> listOf(spec.transform(arguments.initialValues))
                is AddressSpec -> spec.transform(
                    arguments.initialValues,
                    arguments.shippingValues,
                    arguments.autocompleteAddressInteractorFactory,
                )
                is PlaceholderSpec -> listOf() // Placeholders should be processed before calling transform.
            }
        }
    }
}
