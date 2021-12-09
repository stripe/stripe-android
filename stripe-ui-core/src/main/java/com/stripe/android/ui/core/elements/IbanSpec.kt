package com.stripe.android.ui.core.elements

internal object IbanSpec : SectionFieldSpec(IdentifierSpec.Generic("iban")) {
    fun transform(): SectionFieldElement =
        IbanElement(
            this.identifier,
            TextFieldController(IbanConfig())
        )
}
