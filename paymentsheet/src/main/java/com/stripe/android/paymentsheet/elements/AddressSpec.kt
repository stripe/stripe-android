package com.stripe.android.paymentsheet.elements

import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AddressSpec(
    override val identifier: IdentifierSpec,
) : SectionFieldSpec(identifier)
