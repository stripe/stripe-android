package com.stripe.android.paymentsheet.elements

internal sealed class SectionMultiFieldElement(
    override val identifier: IdentifierSpec,
) : SectionFieldElement
