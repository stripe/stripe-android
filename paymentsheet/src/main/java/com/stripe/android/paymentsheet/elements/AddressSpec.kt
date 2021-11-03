package com.stripe.android.paymentsheet.elements

internal data class AddressSpec(
    override val identifier: IdentifierSpec,
) : SectionFieldSpec(identifier)
