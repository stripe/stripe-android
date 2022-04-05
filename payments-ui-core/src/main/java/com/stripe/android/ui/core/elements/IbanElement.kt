package com.stripe.android.ui.core.elements

internal data class IbanElement(
    override val identifier: IdentifierSpec,
    override val controller: TextFieldController
) : SectionSingleFieldElement(identifier)
