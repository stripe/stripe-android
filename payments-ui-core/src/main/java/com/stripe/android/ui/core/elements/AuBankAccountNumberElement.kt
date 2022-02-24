package com.stripe.android.ui.core.elements

internal data class AuBankAccountNumberElement(
    override val identifier: IdentifierSpec,
    override val controller: TextFieldController
) : SectionSingleFieldElement(identifier)
