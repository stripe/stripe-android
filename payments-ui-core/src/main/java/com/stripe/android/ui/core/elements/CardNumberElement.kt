package com.stripe.android.ui.core.elements

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionSingleFieldElement

internal data class CardNumberElement(
    val _identifier: IdentifierSpec,
    override val controller: CardNumberController
) : SectionSingleFieldElement(_identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // Nothing from FormArguments to populate
    }
}
