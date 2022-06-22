package com.stripe.android.ui.core.elements

internal data class CardNumberElement(
    val _identifier: IdentifierSpec,
    override val controller: CardNumberController
) : SectionSingleFieldElement(_identifier) {
    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // Nothing from formFragmentArguments to populate
    }
}
