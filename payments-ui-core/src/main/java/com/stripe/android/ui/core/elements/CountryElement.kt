package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.getValue

internal data class CountryElement(
    override val identifier: IdentifierSpec,
    override val controller: DropdownFieldController
) : SectionSingleFieldElement(identifier) {
    override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
        formFragmentArguments.getValue(identifier)?.let { controller.onRawValueChange(it) }
    }
}
