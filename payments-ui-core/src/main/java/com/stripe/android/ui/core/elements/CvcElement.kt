package com.stripe.android.ui.core.elements

internal data class CvcElement(
    val _identifier: IdentifierSpec,
    override val controller: CvcController
) : SectionSingleFieldElement(_identifier) {
    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // Nothing from formFragmentArguments to populate
    }
}
