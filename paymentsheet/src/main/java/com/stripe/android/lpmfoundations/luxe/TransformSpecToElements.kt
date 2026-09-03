package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.specsForConfiguration
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.LayoutSpec
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
    @Suppress("UnusedParameter")
    fun transform(
        metadata: PaymentMethodMetadata?,
        specs: List<FormItemSpec>,
        placeholderOverrideList: List<IdentifierSpec> = emptyList(),
        termsDisplay: PaymentSheet.TermsDisplay,
    ): List<FormElement> {
        return specsForConfiguration(
            configuration = arguments.billingDetailsCollectionConfiguration,
            placeholderOverrideList = placeholderOverrideList,
            requiresMandate = arguments.requiresMandate,
            specs = specs,
            termsDisplay = termsDisplay,
        ).flatMap { emptyList() }
    }
}
