package com.stripe.android.paymentsheet.elements

internal data class CvcTextElement(
    val _identifier: IdentifierSpec,
    override val controller: CvcTextFieldController,
) : SectionSingleFieldElement(_identifier)


