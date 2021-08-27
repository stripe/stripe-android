package com.stripe.android.paymentsheet.elements

internal data class CardNumberTextElement(
    val _identifier: IdentifierSpec,
    override val controller: CreditNumberTextFieldController,
) : SectionSingleFieldElement(_identifier)
