package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments

internal data class CardNumberElement(
    val _identifier: IdentifierSpec,
    override val controller: CardNumberController,
) : SectionSingleFieldElement(_identifier) {
    override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
        // Nothing from formFragmentArguments to populate
    }
}
